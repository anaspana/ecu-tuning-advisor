package com.ftn.sbnz.service.tests;

import com.ftn.sbnz.model.enums.EcuType;
import com.ftn.sbnz.model.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testovi za Accumulate mehanizam - analiza habanja motora
 * na osnovu istorijskih ocitavanja opterecenja.
 *
 * Granica: prosecno opterecenje >75% -> HIGH_WEAR -> blokada Stage1 / Stage2
 */
public class AccumulateTest {

    private KieSession kieSession;

    @BeforeEach
    public void setup(TestInfo testInfo) {
        System.out.println("\n===============================================================");
        System.out.println("TEST: " + testInfo.getDisplayName());
        System.out.println("===============================================================");
        KieServices ks = KieServices.Factory.get();
        KieContainer kc = ks.getKieClasspathContainer();
        kieSession = kc.newKieSession("forwardKsession");
    }

    @BeforeEach
    public void teardown() {
        if (kieSession != null) kieSession.dispose();
    }

    // pomocna metoda za generisanje ocitavanja
    private List<EngineLoadReading> generateReadings(String vehicleId, double... loads) {
        List<EngineLoadReading> readings = new ArrayList<>();
        for (int i = 0; i < loads.length; i++) {
            readings.add(new EngineLoadReading(
                    vehicleId,
                    loads[i],
                    LocalDate.now().minusDays(loads.length - i)
            ));
        }
        return readings;
    }

    private VehicleProfile defaultProfile(String vehicleId) {
        VehicleProfile p = new VehicleProfile();
        p.setVehicleId(vehicleId);
        p.setEcuType(EcuType.BOSCH_EDC17);
        p.setVehicleBrand("VW");
        p.setMileage(180000);
        p.setMaxBoostPressure(1.8);
        p.setMaxExhaustTemp(850.0);
        return p;
    }

    // -------------------------------------------------------------------
    // HIGH_WEAR testovi
    // -------------------------------------------------------------------

    @Test
    public void test_HighWear_ProsecinaIznad75() {
        // prosek: (80+85+90+82+88) / 5 = 85.0% -> HIGH_WEAR
        List<EngineLoadReading> readings = generateReadings("VW-001", 80, 85, 90, 82, 88);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-001"));
        kieSession.fireAllRules();

        Optional<EngineWearStatus> status = kieSession.getObjects().stream()
                .filter(f -> f instanceof EngineWearStatus)
                .map(f -> (EngineWearStatus) f)
                .findFirst();

        assertTrue(status.isPresent());
        assertEquals(EngineWearStatus.WearLevel.HIGH_WEAR, status.get().getWearLevel());
        System.out.println("Prosek: " + status.get().getAverageLoadPercent() + "%");
    }

    @Test
    public void test_HighWear_BlokiranStage1() {
        // prosek >75% + zahtev Stage1 -> TuningBlockedDecision
        List<EngineLoadReading> readings = generateReadings("VW-002", 80, 85, 90);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-002"));

        TuningRequest req = new TuningRequest(
                "VW-002", EcuType.BOSCH_EDC17, "VW",
                1.7, 840.0, 380.0, "Stage1");
        kieSession.insert(req);
        kieSession.fireAllRules();

        Optional<TuningBlockedDecision> blocked = kieSession.getObjects().stream()
                .filter(f -> f instanceof TuningBlockedDecision)
                .map(f -> (TuningBlockedDecision) f)
                .findFirst();

        assertTrue(blocked.isPresent());
        assertEquals("Stage1", blocked.get().getBlockedStage());
        System.out.println("Blokada: " + blocked.get().getReason());
    }

    @Test
    public void test_HighWear_BlokiranStage2() {
        // prosek >75% + zahtev Stage2 -> blokada
        List<EngineLoadReading> readings = generateReadings("VW-003", 78, 82, 91, 85);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-003"));

        TuningRequest req = new TuningRequest(
                "VW-003", EcuType.BOSCH_EDC17, "VW",
                2.0, 860.0, 410.0, "Stage2");
        kieSession.insert(req);
        kieSession.fireAllRules();

        Optional<TuningBlockedDecision> blocked = kieSession.getObjects().stream()
                .filter(f -> f instanceof TuningBlockedDecision)
                .map(f -> (TuningBlockedDecision) f)
                .findFirst();

        assertTrue(blocked.isPresent());
        assertEquals("Stage2", blocked.get().getBlockedStage());
        System.out.println("Blokada: " + blocked.get().getReason());
    }

    @Test
    public void test_HighWear_EcoNijeBlokiran() {
        // prosek >75% + zahtev Eco -> NIJE blokiran (Eco je bezbedna opcija)
        List<EngineLoadReading> readings = generateReadings("VW-004", 80, 85, 90);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-004"));

        TuningRequest req = new TuningRequest(
                "VW-004", EcuType.BOSCH_EDC17, "VW",
                1.5, 820.0, 350.0, "Eco");
        kieSession.insert(req);
        kieSession.fireAllRules();

        Optional<TuningBlockedDecision> blocked = kieSession.getObjects().stream()
                .filter(f -> f instanceof TuningBlockedDecision)
                .map(f -> (TuningBlockedDecision) f)
                .findFirst();

        assertFalse(blocked.isPresent(), "Eco mapa ne sme biti blokirana");
        System.out.println("Eco mapa nije blokirana - ispravno.");
    }

    @Test
    public void test_HighWear_EcoMapaPreporucena() {
        // HIGH_WEAR -> automatska preporuka Eco mape (bez TuningRequest)
        List<EngineLoadReading> readings = generateReadings("VW-005", 80, 85, 90);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-005"));
        kieSession.fireAllRules();

        Optional<TuningAction> ecoAction = kieSession.getObjects().stream()
                .filter(f -> f instanceof TuningAction)
                .map(f -> (TuningAction) f)
                .filter(a -> "ECO_MAP_RECOMMENDED".equals(a.getActionType()))
                .findFirst();

        assertTrue(ecoAction.isPresent());
        System.out.println("Eco preporuka: " + ecoAction.get().getDescription());
    }

    // -------------------------------------------------------------------
    // NORMAL_WEAR testovi
    // -------------------------------------------------------------------

    @Test
    public void test_NormalWear_ProsecinaIspod75() {
        // prosek: (60+65+70+68) / 4 = 65.75% -> NORMAL_WEAR
        List<EngineLoadReading> readings = generateReadings("BMW-001", 60, 65, 70, 68);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("BMW-001"));
        kieSession.fireAllRules();

        Optional<EngineWearStatus> status = kieSession.getObjects().stream()
                .filter(f -> f instanceof EngineWearStatus)
                .map(f -> (EngineWearStatus) f)
                .findFirst();

        assertTrue(status.isPresent());
        assertEquals(EngineWearStatus.WearLevel.NORMAL_WEAR, status.get().getWearLevel());
        System.out.println("Prosek: " + status.get().getAverageLoadPercent() + "%");
    }

    @Test
    public void test_NormalWear_Stage1Dozvoljen() {
        // NORMAL_WEAR + Stage1 -> nema blokade
        List<EngineLoadReading> readings = generateReadings("BMW-002", 60, 65, 70);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("BMW-002"));

        TuningRequest req = new TuningRequest(
                "BMW-002", EcuType.BOSCH_EDC17, "BMW",
                1.7, 840.0, 380.0, "Stage1");
        kieSession.insert(req);
        kieSession.fireAllRules();

        Optional<TuningBlockedDecision> blocked = kieSession.getObjects().stream()
                .filter(f -> f instanceof TuningBlockedDecision)
                .map(f -> (TuningBlockedDecision) f)
                .findFirst();

        assertFalse(blocked.isPresent(), "Stage1 ne sme biti blokiran za NORMAL_WEAR motor");
        System.out.println("Stage1 dozvoljen za NORMAL_WEAR - ispravno.");
    }

    @Test
    public void test_GranicnaVrednost_TacnoNa75() {
        // prosek tacno 75% -> NORMAL_WEAR (granica je strogo > 75)
        List<EngineLoadReading> readings = generateReadings("VW-010", 75.0, 75.0, 75.0);
        readings.forEach(kieSession::insert);
        kieSession.insert(defaultProfile("VW-010"));
        kieSession.fireAllRules();

        Optional<EngineWearStatus> status = kieSession.getObjects().stream()
                .filter(f -> f instanceof EngineWearStatus)
                .map(f -> (EngineWearStatus) f)
                .findFirst();

        assertTrue(status.isPresent());
        assertEquals(EngineWearStatus.WearLevel.NORMAL_WEAR, status.get().getWearLevel(),
                "Tacno 75% je granicna vrednost - mora biti NORMAL_WEAR");
        System.out.println("Granicna vrednost 75%: NORMAL_WEAR - ispravno.");
    }
}