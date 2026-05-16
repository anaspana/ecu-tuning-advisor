package com.ftn.sbnz.service;

import com.ftn.sbnz.model.dto.TuningValidateRequest;
import com.ftn.sbnz.model.dto.TuningValidateResponse;
import com.ftn.sbnz.model.models.MissingComponent;
import com.ftn.sbnz.model.models.TuningDecision;
import com.ftn.sbnz.model.models.TuningRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

// Controller za validaciju zahteva za tuning.
// Kombinuje dva Drools mehanizma u jednoj akciji:
//   1. Template pravila - proveravaju fizicke limite ECU-a (max boost, temp, torque)
//   2. Backward Chaining - proverava hardverske preduslove (komponente vozila)

// Finalna odluka je ALLOW samo ako oba mehanizma prolaze.

@RestController
@RequestMapping("/api/tuning")
@CrossOrigin
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;
    private final BackwardChainingService backwardChainingService;

    @Autowired
    public TemplateController(TemplateService templateService,
                               BackwardChainingService backwardChainingService) {
        this.templateService = templateService;
        this.backwardChainingService = backwardChainingService;
    }

     //POST /api/tuning/validate
     //Proverava da li su trazeni parametri bezbedni za dati ECU/marku (Template)
     //i da li vozilo ima sve potrebne hardverske komponente (Backward Chaining).
     
     // Primer request body:
     // {
     //  "vehicleId": "VW-001",
     //  "ecuType": "BOSCH_EDC17",
     //  "brand": "VW",
     //  "tuningStage": "Stage2Tuning",
     //  "requestedBoost": 2.1,
     //  "requestedTemp": 840,
     //  "requestedTorque": 380,
     //  "vehicleComponents": ["HybridTurbine", "Downpipe"]
     // }
    @PostMapping("/validate")
    public ResponseEntity<TuningValidateResponse> validate(@RequestBody TuningValidateRequest request) {
        log.info("Primljen zahtev za validaciju tuninga: vozilo={}, stage={}, ECU={}/{}",
                request.getVehicleId(), request.getTuningStage(),
                request.getEcuType(), request.getBrand());

        List<String> messages = new ArrayList<>();

        // Template pravila (fizicki limiti)
        TuningRequest tuningRequest = new TuningRequest(
                request.getVehicleId(),
                request.getEcuType(),
                request.getBrand(),
                request.getRequestedBoost(),
                request.getRequestedTemp(),
                request.getRequestedTorque(),
                request.getTuningStage()
        );
        TuningDecision templateDecision = templateService.evaluateTuning(tuningRequest);
        messages.add("[Template] " + templateDecision.getReason());

        // Backward Chaining (hardverski preduslovi)
        List<MissingComponent> missingComponents = backwardChainingService.checkPrerequisites(
                request.getVehicleId(),
                request.getTuningStage(),
                request.getVehicleComponents()
        );
        missingComponents.forEach(mc ->
            messages.add("[Backward] Nedostaje komponenta: " + mc.getComponentName()
                + " (potrebna za: " + mc.getRequiredForStage() + ")")
        );

        // Finalna odluka: ALLOW samo ako su oba mehanizma OK
        boolean templateOk = templateDecision.getDecision() == TuningDecision.Decision.ALLOW;
        boolean backwardOk = missingComponents.isEmpty();
        TuningDecision.Decision finalDecision =
                (templateOk && backwardOk) ? TuningDecision.Decision.ALLOW : TuningDecision.Decision.BLOCK;

        if (finalDecision == TuningDecision.Decision.ALLOW) {
            messages.add("Tuning odobren - svi uslovi su ispunjeni.");
        } else {
            messages.add("Tuning blokiran - pogledaj razloge iznad.");
        }

        log.info("Finalna odluka za {}: {} (template={}, backward={})",
                request.getVehicleId(), finalDecision, templateOk, backwardOk);

        TuningValidateResponse response = new TuningValidateResponse(
                request.getVehicleId(),
                finalDecision,
                templateDecision,
                missingComponents,
                messages
        );

        return ResponseEntity.ok(response);
    }
}