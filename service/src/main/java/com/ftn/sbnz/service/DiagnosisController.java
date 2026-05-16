package com.ftn.sbnz.service;

import com.ftn.sbnz.model.dto.AccumulateRequest;
import com.ftn.sbnz.model.dto.AccumulateResponse;
import com.ftn.sbnz.model.dto.DiagnosisRequest;
import com.ftn.sbnz.model.dto.DiagnosisResponse;
import com.ftn.sbnz.model.enums.EcuType;
import com.ftn.sbnz.model.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// Controller za dijagnostiku vozila
// FC (DTC kodovi) i Accumulate (istorija voznji)

@RestController
@RequestMapping("/api/diagnosis")
@CrossOrigin
public class DiagnosisController {
        private static final Logger log = LoggerFactory.getLogger(DiagnosisController.class);

        private final DiagnosisService diagnosisService;
        private final AccumulateService accumulateService;

        @Autowired
        public DiagnosisController(DiagnosisService diagnosisService,
                                        AccumulateService accumulateService) {
                this.diagnosisService = diagnosisService;
                this.accumulateService = accumulateService;
        }

        // POST /api/diagnosis
        // Prima podatke o vozilu i DTC kodove, pokrece Drools Forward Chaining sesiju
        // i vraca dijagnostiku, preporucene akcije i alate
        // Primer request body:
        // {
        //   "vehicleId": "VW-001",
        //   "ecuType": "BOSCH_EDC17",
        // "vehicleBrand": "Volkswagen",
        // "mileage": 180000,
        // "maxGearboxTorque": 400,
        // "dtcCodes": ["P0401", "P242F"]
        // }
        @PostMapping
        public ResponseEntity<DiagnosisResponse> diagnose(@RequestBody DiagnosisRequest request) {
                log.info("Primljen zahtev za dijagnostiku vozila: {}", request.getVehicleId());
                DiagnosisResponse response = diagnosisService.diagnose(request);
                return ResponseEntity.ok(response);
        }

        // POST /api/diagnosis/history
        //
        // Prima istorijska ocitavanja opterecenja motora i pokrece Accumulate analizu.
        // Ako je prosecno opterecenje > 75%, motor se klasifikuje kao HIGH_WEAR
        // i agresivni tuning (Stage1/Stage2) se blokira.
        //
        // Primer request body:
        // {
        //   "vehicleId": "VW-001",
        //   "engineLoadHistory": [82, 79, 85, 91, 76, 88, 70]
        // }
        //
        // Primer response:
        // {
        //   "vehicleId": "VW-001",
        //   "averageLoadPercent": 81.57,
        //   "wearLevel": "HIGH_WEAR",
        //   "tuningBlocked": true,
        //   "message": "Motor pokazuje visoko prosecno opterecenje (>75%). Preporucuje se iskljucivo Eco mapa."
        // }
        @PostMapping("/history")
        public ResponseEntity<AccumulateResponse> analyzeHistory(@RequestBody AccumulateRequest request) {
        log.info("Primljen zahtev za analizu istorije vozila: {} ({} ocitavanja)",
                request.getVehicleId(), request.getEngineLoadHistory().size());

        // Konvertuj listu Double vrednosti u EngineLoadReading objekte
        List<EngineLoadReading> readings = request.getEngineLoadHistory().stream()
                .map(load -> new EngineLoadReading(request.getVehicleId(), load, LocalDate.now()))
                .collect(Collectors.toList());

        // Pokreni Accumulate analizu sa neutralnim TuningRequest-om
        // (Stage1 jer Accumulate pravilo treba tuningStage da bi odlucilo o blokadi)
        TuningRequest tuningRequest = new TuningRequest(
                request.getVehicleId(),
                EcuType.BOSCH_EDC17,  // placeholder, accumulate ne koristi ECU tip
                "VW",
                0, 0, 0,
                "Stage1"
        );

        VehicleProfile profile = new VehicleProfile();
        profile.setVehicleId(request.getVehicleId());

        AccumulateService.AccumulateResult result =
                accumulateService.evaluateEngineWear(readings, tuningRequest, profile);

        // Izgradi response
        double avg = request.getEngineLoadHistory().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        EngineWearStatus.WearLevel wearLevel = result.wearStatus != null
                ? result.wearStatus.getWearLevel()
                : EngineWearStatus.WearLevel.NORMAL_WEAR;

        boolean blocked = result.isTuningBlocked();

        String message = blocked
                ? "Motor pokazuje visoko prosečno opterećenje (>75%). Preporucuje se isključivo Eco mapa radi očuvanja motora."
                : "Motor je u dobrom stanju. Prosecno opterećenje je ispod praga habanja.";

        AccumulateResponse response = new AccumulateResponse(
                request.getVehicleId(),
                Math.round(avg * 100.0) / 100.0,
                wearLevel,
                blocked,
                message
        );

        log.info("Analiza zavrsena: vozilo={}, prosek={}%, wear={}, blocked={}",
                request.getVehicleId(), response.getAverageLoadPercent(), wearLevel, blocked);

        return ResponseEntity.ok(response);
        }
}