package com.ftn.sbnz.service;

import com.ftn.sbnz.model.events.*;
import com.ftn.sbnz.model.models.ActiveLimits;
import com.ftn.sbnz.model.models.EngineStatus;
import com.ftn.sbnz.model.models.TuningDecision;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.time.SessionPseudoClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CEPService {

    private static final Logger log = LoggerFactory.getLogger(CEPService.class);

    // ksession za testove - pseudo clock, vreme se kontrolise rucno
    // koriste je CEPTest i simulateExhaustTemp/simulateOilPressure kad je usePseudoClock=true
    public static final String CEP_SESSION_PSEUDO   = "cepKsession";

    // ksession za frontend / realtime pozive
    // koristi sistemski sat - svaki insertovani event nosi tacno vreme kada je primljen
    public static final String CEP_SESSION_REALTIME = "cepKsessionRealtime";

    private final KieContainer kieContainer;
    private final NotificationService notificationService;

    @Autowired
    public CEPService(KieContainer kieContainer, NotificationService notificationService) {
        this.kieContainer = kieContainer;
        this.notificationService = notificationService;
    }

    public ActiveLimits createActiveLimits(String vehicleId, TuningDecision decision,
                                            double maxBoost, double maxTemp, double maxTorque) {
        return new ActiveLimits(vehicleId, maxTemp, maxBoost, maxTorque);
    }

    // Simulira stream temperature izduvnih gasova
    //
    // @param usePseudoClock true  = testovi (pseudo clock, rucno pomera vreme po offsetMs)
    //                       false = frontend (realtime clock, koristi System.currentTimeMillis)
    public CEPResult simulateExhaustTemp(String vehicleId,
                                          ActiveLimits limits,
                                          List<TempReading> events,
                                          boolean usePseudoClock) {
        String sessionName = usePseudoClock ? CEP_SESSION_PSEUDO : CEP_SESSION_REALTIME;
        KieSession kieSession = kieContainer.newKieSession(sessionName);

        try {
            kieSession.setGlobal("notificationService", notificationService);
            kieSession.insert(limits);

            if (usePseudoClock) {
                // Testovi: pseudo clock, vreme se pomera po offsetMs iz TempReading
                SessionPseudoClock clock = kieSession.getSessionClock();
                for (TempReading reading : events) {
                    if (reading.offsetMs > 0) {
                        clock.advanceTime(reading.offsetMs, TimeUnit.MILLISECONDS);
                    }
                    kieSession.insert(new ExhaustTempEvent(
                        vehicleId, reading.temperature, clock.getCurrentTime()));
                    kieSession.fireAllRules();
                }
            } else {
                // Frontend: realtime clock, event nosi System.currentTimeMillis
                for (TempReading reading : events) {
                    kieSession.insert(new ExhaustTempEvent(
                        vehicleId, reading.temperature, System.currentTimeMillis()));
                    kieSession.fireAllRules();
                }
            }

            return extractResult(vehicleId, kieSession);
        } finally {
            kieSession.dispose();
        }
    }

    // Overload za unazad kompatibilnost sa testovima koji pozivaju bez usePseudoClock
    // Testovi pozivaju simulateExhaustTemp(vehicleId, limits, readings) - podrazumeva pseudo
    public CEPResult simulateExhaustTemp(String vehicleId,
                                          ActiveLimits limits,
                                          List<TempReading> events) {
        return simulateExhaustTemp(vehicleId, limits, events, true);
    }

    // Simulira stream pritiska ulja
    // @param usePseudoClock true = testovi, false = frontend/realtime
    public CEPResult simulateOilPressure(String vehicleId,
                                          ActiveLimits limits,
                                          List<OilReading> events,
                                          boolean usePseudoClock) {
        String sessionName = usePseudoClock ? CEP_SESSION_PSEUDO : CEP_SESSION_REALTIME;
        KieSession kieSession = kieContainer.newKieSession(sessionName);

        try {
            kieSession.setGlobal("notificationService", notificationService);
            kieSession.insert(limits);

            if (usePseudoClock) {
                SessionPseudoClock clock = kieSession.getSessionClock();
                for (OilReading reading : events) {
                    clock.advanceTime(reading.offsetMs, TimeUnit.MILLISECONDS);
                    kieSession.insert(new OilPressureEvent(
                        vehicleId, reading.pressureBar, reading.rpm, clock.getCurrentTime()));
                    kieSession.fireAllRules();
                }
            } else {
                for (OilReading reading : events) {
                    kieSession.insert(new OilPressureEvent(
                        vehicleId, reading.pressureBar, reading.rpm, System.currentTimeMillis()));
                    kieSession.fireAllRules();
                }
            }

            return extractResult(vehicleId, kieSession);
        } finally {
            kieSession.dispose();
        }
    }

    // Overload za unazad kompatibilnost sa testovima
    public CEPResult simulateOilPressure(String vehicleId,
                                          ActiveLimits limits,
                                          List<OilReading> events) {
        return simulateOilPressure(vehicleId, limits, events, true);
    }

    private CEPResult extractResult(String vehicleId, KieSession kieSession) {
        Collection<?> facts = kieSession.getObjects();

        Optional<EngineStatus> status = facts.stream()
                .filter(f -> f instanceof EngineStatus)
                .map(f -> (EngineStatus) f)
                .filter(s -> s.getVehicleId().equals(vehicleId))
                .findFirst();

        boolean thermalOverload = facts.stream()
                .anyMatch(f -> f instanceof ThermalOverloadEvent &&
                               ((ThermalOverloadEvent) f).getVehicleId().equals(vehicleId));

        boolean engineCritical = facts.stream()
                .anyMatch(f -> f instanceof EngineCriticalEvent &&
                               ((EngineCriticalEvent) f).getVehicleId().equals(vehicleId));

        long highTempWarnings = facts.stream()
                .filter(f -> f instanceof HighTempWarning &&
                             ((HighTempWarning) f).getVehicleId().equals(vehicleId))
                .count();

        return new CEPResult(vehicleId, status.orElse(null),
                thermalOverload, engineCritical,
                (int) highTempWarnings,
                notificationService.getNotifications());
    }

    // DTO klase
    public static class TempReading {
        public final double temperature;
        public final long offsetMs;

        public TempReading(double temperature, long offsetMs) {
            this.temperature = temperature;
            this.offsetMs = offsetMs;
        }
    }

    public static class OilReading {
        public final double pressureBar;
        public final int rpm;
        public final long offsetMs;

        public OilReading(double pressureBar, int rpm, long offsetMs) {
            this.pressureBar = pressureBar;
            this.rpm = rpm;
            this.offsetMs = offsetMs;
        }
    }

    public static class CEPResult {
        public final String vehicleId;
        public final EngineStatus engineStatus;
        public final boolean thermalOverloadTriggered;
        public final boolean engineCriticalTriggered;
        public final int highTempWarningCount;
        public final List<String> notifications;

        public CEPResult(String vehicleId, EngineStatus engineStatus,
                         boolean thermalOverloadTriggered, boolean engineCriticalTriggered,
                         int highTempWarningCount, List<String> notifications) {
            this.vehicleId = vehicleId;
            this.engineStatus = engineStatus;
            this.thermalOverloadTriggered = thermalOverloadTriggered;
            this.engineCriticalTriggered = engineCriticalTriggered;
            this.highTempWarningCount = highTempWarningCount;
            this.notifications = notifications;
        }

        public boolean isSafeMode() {
            return engineStatus != null &&
                   engineStatus.getStatus() == EngineStatus.Status.SAFE_MODE;
        }

        public boolean isCritical() {
            return engineStatus != null &&
                   engineStatus.getStatus() == EngineStatus.Status.CRITICAL;
        }
    }
}