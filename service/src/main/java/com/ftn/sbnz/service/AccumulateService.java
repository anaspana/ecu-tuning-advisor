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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AccumulateService {

    private static final Logger log = LoggerFactory.getLogger(AccumulateService.class);

    private final KieContainer kieContainer;

    @Autowired
    public AccumulateService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    // Analizira istorijska ocitavanja opterecenja motora i procenjuje habanje
    // Ako je prosek >75% i korisnik trazi Stage 1/2, vraca TuningBlockedDecision
    //
    // @param readings   lista istorijskih ocitavanja
    // @param request    zahtev za tuning (sa tuningStage poljem)
    // @param profile    profil vozila (za forward chaining pravila)
    // @return           rezultat sa statusom habanja i eventualnom blokadom

    public AccumulateResult evaluateEngineWear(
            List<EngineLoadReading> readings,
            TuningRequest request,
            VehicleProfile profile) {

        log.info("Analiza habanja motora za vozilo: {} ({} ocitavanja, zahtev: {})",
                request.getVehicleId(), readings.size(), request.getTuningStage());

        KieSession kieSession = kieContainer.newKieSession("forwardKsession");

        try {
            // Ubaci profil vozila
            kieSession.insert(profile);

            // Ubaci sva historijska ocitavanja
            readings.forEach(kieSession::insert);

            // Ubaci zahtev za tuning
            kieSession.insert(request);

            kieSession.fireAllRules();

            Collection<?> facts = kieSession.getObjects();

            // Pokupi status habanja
            Optional<EngineWearStatus> wearStatus = facts.stream()
                    .filter(f -> f instanceof EngineWearStatus)
                    .map(f -> (EngineWearStatus) f)
                    .filter(w -> w.getVehicleId().equals(request.getVehicleId()))
                    .findFirst();

            // Pokupi eventualnu blokadu
            Optional<TuningBlockedDecision> blocked = facts.stream()
                    .filter(f -> f instanceof TuningBlockedDecision)
                    .map(f -> (TuningBlockedDecision) f)
                    .filter(b -> b.getVehicleId().equals(request.getVehicleId()))
                    .findFirst();

            // Pokupi eco preporuku ako postoji
            List<TuningAction> actions = facts.stream()
                    .filter(f -> f instanceof TuningAction)
                    .map(f -> (TuningAction) f)
                    .filter(a -> a.getVehicleId().equals(request.getVehicleId()))
                    .collect(Collectors.toList());

            log.info("Rezultat: wearStatus={}, blocked={}",
                    wearStatus.map(w -> w.getWearLevel().toString()).orElse("N/A"),
                    blocked.isPresent());

            return new AccumulateResult(
                    request.getVehicleId(),
                    wearStatus.orElse(null),
                    blocked.orElse(null),
                    actions
            );

        } finally {
            kieSession.dispose();
        }
    }

    // DTO za rezultat analize habanja motora
    public static class AccumulateResult {
        public final String vehicleId;
        public final EngineWearStatus wearStatus;
        public final TuningBlockedDecision blockedDecision;
        public final List<TuningAction> recommendedActions;

        public AccumulateResult(String vehicleId, EngineWearStatus wearStatus,
                                TuningBlockedDecision blockedDecision,
                                List<TuningAction> recommendedActions) {
            this.vehicleId = vehicleId;
            this.wearStatus = wearStatus;
            this.blockedDecision = blockedDecision;
            this.recommendedActions = recommendedActions;
        }

        public boolean isTuningBlocked() {
            return blockedDecision != null;
        }
    }
}