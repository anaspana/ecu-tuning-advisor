package com.ftn.sbnz.service;

import com.ftn.sbnz.model.models.TuningDecision;
import com.ftn.sbnz.model.models.TuningRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tuning")
@CrossOrigin
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;

    @Autowired
    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * POST /api/tuning/evaluate
     *
     * Proverava da li su trazeni parametri bezbedni za dati ECU/marku.
     *
     * Primer request body:
     * {
     *   "vehicleId": "VW-001",
     *   "ecuType": "BOSCH_EDC17",
     *   "brand": "VW",
     *   "requestedBoost": 2.1,
     *   "requestedTemp": 840,
     *   "requestedTorque": 380
     * }
     *
     * Odgovor: { vehicleId, decision: "BLOCK"/"ALLOW", reason, profileKey }
     */
    @PostMapping("/evaluate")
    public ResponseEntity<TuningDecision> evaluate(@RequestBody TuningRequest request) {
        log.info("Primljen zahtev za evaluaciju tuninga: {}", request);
        TuningDecision decision = templateService.evaluateTuning(request);
        return ResponseEntity.ok(decision);
    }
}
