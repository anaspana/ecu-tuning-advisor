package com.ftn.sbnz.service;

import com.ftn.sbnz.model.models.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

// Servis koji pokrece Backward Chaining sesiju i proverava da li vozilo
// ima sve hardverske preduslove za trazeni nivo tuninga.

// Hijerarhija preduslova (staticka baza znanja):
//  HybridTurbine  -> potrebna za Stage2Tuning
//   Downpipe       -> potreban za Stage2Tuning
//   Intercooler    -> potreban za HybridTurbine (indirektan preduslov za Stage2)
//   Stage2Tuning   -> potreban za HighPerformanceTuning
//  Downpipe       -> potreban za HighPerformanceTuning (direktan)

// Query rekurzivno prolazi kroz celu hijerarhiju i detektuje sve
// nedostajuce komponente, ukljucujuci indirektne preduslove.
 
@Service
public class BackwardChainingService {

    private static final Logger log = LoggerFactory.getLogger(BackwardChainingService.class);

    private final KieContainer kieContainer;

    @Autowired
    public BackwardChainingService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    // Proverava hardverske preduslove za trazeni nivo tuninga.
    //
    // @param vehicleId        ID vozila
    // @param tuningStage      trazeni nivo (npr. "Stage2Tuning", "HighPerformanceTuning")
    // @param vehicleComponents lista komponenti koje vozilo ima ugradene
    // @return lista MissingComponent objekata (prazna ako su svi preduslovi ispunjeni)
    
    public List<MissingComponent> checkPrerequisites(String vehicleId,
                                                      String tuningStage,
                                                      List<String> vehicleComponents) {
        log.info("Backward Chaining provera preduslova za vozilo: {} (nivo: {})", vehicleId, tuningStage);

        KieSession kieSession = kieContainer.newKieSession("bwKsession");

        try {
            // ubaci staticku bazu znanja - hijerarhiju hardverskih preduslova
            insertPrerequisiteKnowledgeBase(kieSession);

            // Ubaci komponente koje vozilo ima
            if (vehicleComponents != null) {
                vehicleComponents.forEach(component ->
                    kieSession.insert(new VehicleHasComponent(vehicleId, component))
                );
            }

            // ubaci zahtev za tuning - pravilo ce reagovati na tuningStage
            TuningRequest request = new TuningRequest(
                vehicleId, null, null,
                0, 0, 0,
                tuningStage
            );
            kieSession.insert(request);

            kieSession.fireAllRules();

            // Pokupi sve detektovane nedostajuce komponente
            Collection<?> facts = kieSession.getObjects();
            List<MissingComponent> missing = facts.stream()
                    .filter(f -> f instanceof MissingComponent)
                    .map(f -> (MissingComponent) f)
                    .filter(m -> m.getVehicleId().equals(vehicleId))
                    .collect(Collectors.toList());

            if (missing.isEmpty()) {
                log.info("Vozilo {} ima sve hardverske preduslove za {}.", vehicleId, tuningStage);
            } else {
                log.info("Vozilu {} nedostaju komponente za {}: {}",
                    vehicleId, tuningStage,
                    missing.stream().map(MissingComponent::getComponentName).collect(Collectors.joining(", ")));
            }

            return missing;

        } finally {
            kieSession.dispose();
        }
    }

    // Ubacuje staticku bazu znanja hardverskih preduslova u sesiju
    // Ova hijerarhija se ne menja - definisana je domenom ECU tuninga
    private void insertPrerequisiteKnowledgeBase(KieSession kieSession) {
        // Direktni preduslovi za Stage2Tuning
        kieSession.insert(new HardwarePrerequisite("HybridTurbine", "Stage2Tuning"));
        kieSession.insert(new HardwarePrerequisite("Downpipe", "Stage2Tuning"));
        // Intercooler je preduslov za HybridTurbine (indirektan preduslov za Stage2)
        kieSession.insert(new HardwarePrerequisite("Intercooler", "HybridTurbine"));
        // Stage2Tuning je preduslov za HighPerformanceTuning
        kieSession.insert(new HardwarePrerequisite("Stage2Tuning", "HighPerformanceTuning"));
        kieSession.insert(new HardwarePrerequisite("Downpipe", "HighPerformanceTuning"));
    }
}