package com.ftn.sbnz.service;

import com.ftn.sbnz.model.dto.TelemetryEventRequest;
import com.ftn.sbnz.model.dto.TelemetryStatusResponse;
import com.ftn.sbnz.model.events.*;
import com.ftn.sbnz.model.models.ActiveLimits;
import com.ftn.sbnz.model.models.EngineStatus;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Controller za live telemetriju vozila (Complex Event Processing)

// Kljucna arhitekturalna odluka: svako vozilo ima TRAJNU KieSession koja
// zivi sve dok se ne pozove /reset. Ovo je neophodno jer CEP pravila
// (narocito Scenario 2 - pritisak ulja) porede dva dogadjaja razdvojena
// vremenskim intervalom (after[3s, *]) - sto je moguce samo ako su oba
// dogadjaja u istoj sesiji sa realtime clock-om

// Za testove se i dalje koristi CEPService sa pseudo clock-om
@RestController
@RequestMapping("/api/telemetry")
@CrossOrigin
public class TelemetryController {

    private static final Logger log = LoggerFactory.getLogger(TelemetryController.class);

    private static final double DEFAULT_MAX_TEMP   = 850.0;
    private static final double DEFAULT_MAX_BOOST  = 1.8;
    private static final double DEFAULT_MAX_TORQUE = 400.0;

    private final KieContainer kieContainer;
    private final NotificationService notificationService;

    // trajna KieSession po vehicleId - zivi dok se ne resetuje
    private final Map<String, KieSession> sessions = new ConcurrentHashMap<>();
    // Pracenje stanja po vozilu (za response)
    private final Map<String, VehicleState> vehicleStates = new ConcurrentHashMap<>();

    @Autowired
    public TelemetryController(KieContainer kieContainer, NotificationService notificationService) {
        this.kieContainer = kieContainer;
        this.notificationService = notificationService;
    }

    // POST /api/telemetry/event

    // Insertuje jedno ocitavanje senzora u trajnu CEP sesiju vozila.
    // Sesija se kreira pri prvom pozivu i zivi do /reset poziva,
    // sto omogucava CEP pravilima da porede dogadjaje kroz vreme.
    @PostMapping("/event")
    public ResponseEntity<TelemetryStatusResponse> sendEvent(@RequestBody TelemetryEventRequest request) {
        log.info("CEP dogadjaj primljen: vozilo={}, temp={}, oil={}, rpm={}",
                request.getVehicleId(),
                request.getExhaustTempCelsius(),
                request.getOilPressureBar(),
                request.getRpm());

        String vehicleId = request.getVehicleId();
        KieSession session = getOrCreateSession(vehicleId);
        VehicleState state = vehicleStates.computeIfAbsent(vehicleId, id -> new VehicleState());

        // temperatura izduvnih gasova
        if (request.getExhaustTempCelsius() != null) {
            session.insert(new ExhaustTempEvent(vehicleId, request.getExhaustTempCelsius(),
                    System.currentTimeMillis()));
        }

        // pritisak ulja
        if (request.getOilPressureBar() != null && request.getRpm() != null) {
            session.insert(new OilPressureEvent(vehicleId, request.getOilPressureBar(),
                    request.getRpm(), System.currentTimeMillis()));
        }

        session.fireAllRules();

        // procitaj stanje iz sesije nakon okidanja pravila
        updateStateFromSession(session, vehicleId, state);

        return ResponseEntity.ok(buildStatusResponse(vehicleId, state));
    }

    //GET /api/telemetry/status/{vehicleId}
    @GetMapping("/status/{vehicleId}")
    public ResponseEntity<TelemetryStatusResponse> getStatus(@PathVariable String vehicleId) {
        VehicleState state = vehicleStates.getOrDefault(vehicleId, new VehicleState());
        // Osvezi iz sesije ako postoji
        if (sessions.containsKey(vehicleId)) {
            updateStateFromSession(sessions.get(vehicleId), vehicleId, state);
        }
        return ResponseEntity.ok(buildStatusResponse(vehicleId, state));
    }

    // POST /api/telemetry/reset
    // Dispose-uje KieSession i brise stanje za dato vozilo
    // Obavezno pre svake nove demonstracije
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset(@RequestBody Map<String, String> body) {
        String vehicleId = body.get("vehicleId");
        if (vehicleId != null) {
            disposeSession(vehicleId);
            vehicleStates.remove(vehicleId);
            log.info("CEP sesija resetovana za vozilo: {}", vehicleId);
        } else {
            sessions.keySet().forEach(this::disposeSession);
            sessions.clear();
            vehicleStates.clear();
            log.info("CEP sesija resetovana za sva vozila.");
        }
        notificationService.clearNotifications();
        return ResponseEntity.ok(Map.of("status", "reset",
                "vehicleId", vehicleId != null ? vehicleId : "all"));
    }

    // pomocne
    private KieSession getOrCreateSession(String vehicleId) {
        return sessions.computeIfAbsent(vehicleId, id -> {
            log.info("Kreiranje nove CEP sesije za vozilo: {}", id);
            KieSession s = kieContainer.newKieSession(CEPService.CEP_SESSION_REALTIME);
            s.setGlobal("notificationService", notificationService);
            // Insertuj ActiveLimits jednom pri kreiranju sesije
            s.insert(new ActiveLimits(id, DEFAULT_MAX_TEMP, DEFAULT_MAX_BOOST, DEFAULT_MAX_TORQUE));
            return s;
        });
    }

    private void disposeSession(String vehicleId) {
        KieSession s = sessions.remove(vehicleId);
        if (s != null) {
            try { s.dispose(); } catch (Exception e) {
                log.warn("Greska pri dispose sesije za vozilo {}: {}", vehicleId, e.getMessage());
            }
        }
    }

    /// Cita HighTempWarning, ThermalOverloadEvent, EngineCriticalEvent i EngineStatus
    //iz sesije i azurira VehicleState
    private void updateStateFromSession(KieSession session, String vehicleId, VehicleState state) {
        Collection<?> facts = session.getObjects();

        long warnings = facts.stream()
                .filter(f -> f instanceof HighTempWarning
                        && ((HighTempWarning) f).getVehicleId().equals(vehicleId))
                .count();
        state.highTempWarningCount = (int) warnings;

        boolean safeMode = facts.stream()
                .anyMatch(f -> f instanceof EngineStatus
                        && ((EngineStatus) f).getVehicleId().equals(vehicleId)
                        && ((EngineStatus) f).getStatus() == EngineStatus.Status.SAFE_MODE);
        if (safeMode) state.safeModeActive = true;

        boolean oilCritical = facts.stream()
                .anyMatch(f -> f instanceof EngineCriticalEvent
                        && ((EngineCriticalEvent) f).getVehicleId().equals(vehicleId));
        if (oilCritical) state.oilPressureCritical = true;
    }

    private TelemetryStatusResponse buildStatusResponse(String vehicleId, VehicleState state) {
        return new TelemetryStatusResponse(
                vehicleId,
                state.safeModeActive,
                state.highTempWarningCount,
                state.oilPressureCritical,
                new ArrayList<>(notificationService.getNotifications())
        );
    }

    private static class VehicleState {
        boolean safeModeActive    = false;
        int     highTempWarningCount = 0;
        boolean oilPressureCritical  = false;
    }
}