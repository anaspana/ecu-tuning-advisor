package com.ftn.sbnz.service.tests;

import com.ftn.sbnz.model.enums.EcuType;
import com.ftn.sbnz.model.models.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Testovi za Backward Chaining - provera hardverskih preduslova za tuning.
//
// Hijerarhija preduslova:
//   HybridTurbine  -> potrebna za Stage2Tuning
//   Downpipe       -> potreban za Stage2Tuning
//   Intercooler    -> potreban za HybridTurbine  (indirektan preduslov za Stage2)
//   Stage2Tuning   -> potreban za HighPerformanceTuning
//   Downpipe       -> potreban za HighPerformanceTuning (direktan)
//
// Query automatski prolazi kroz celu hijerarhiju rekurzivno.
public class BackwardChainingTest {

    private KieSession kieSession;
    private static final String VEHICLE_ID = "VW-BW-001";

    @BeforeEach
    public void setup(TestInfo testInfo) {
        System.out.println("\n===============================================================");
        System.out.println("TEST: " + testInfo.getDisplayName());
        System.out.println("===============================================================");

        KieServices ks = KieServices.Factory.get();
        KieContainer kc = ks.getKieClasspathContainer();
        kieSession = kc.newKieSession("bwKsession");

        // ubaci poznate veze preduslova (ovo je baza znanja - ne menja se)
        // direktni preduslovi za Stage2Tuning
        kieSession.insert(new HardwarePrerequisite("HybridTurbine", "Stage2Tuning"));
        kieSession.insert(new HardwarePrerequisite("Downpipe", "Stage2Tuning"));
        // preduslov za HybridTurbine (indirektan preduslov za Stage2)
        kieSession.insert(new HardwarePrerequisite("Intercooler", "HybridTurbine"));
        // stage2Tuning je preduslov za HighPerformanceTuning
        kieSession.insert(new HardwarePrerequisite("Stage2Tuning", "HighPerformanceTuning"));
        kieSession.insert(new HardwarePrerequisite("Downpipe", "HighPerformanceTuning"));
    }

    @AfterEach
    public void teardown() {
        if (kieSession != null) kieSession.dispose();
    }

    // pomocne metode
    private List<MissingComponent> getMissingComponents() {
        return kieSession.getObjects().stream()
                .filter(f -> f instanceof MissingComponent)
                .map(f -> (MissingComponent) f)
                .collect(Collectors.toList());
    }

    private TuningRequest stage2Request() {
        return new TuningRequest(VEHICLE_ID, EcuType.BOSCH_EDC17, "VW",
                2.0, 860.0, 410.0, "Stage2Tuning");
    }

    private TuningRequest highPerfRequest() {
        return new TuningRequest(VEHICLE_ID, EcuType.BOSCH_EDC17, "VW",
                2.3, 870.0, 450.0, "HighPerformanceTuning");
    }

    // ---------------------------------------------------------------
    // Testovi za Stage2Tuning
    // ---------------------------------------------------------------

    @Test
    public void test_Stage2_NemaKomponenti_SveNedostaje() {
        // Vozilo nema nista -> sve komponente nedostaju
        // Stage2 trazi: HybridTurbine, Downpipe, i Intercooler (indirektan)
        kieSession.insert(stage2Request());
        kieSession.fireAllRules();

        List<MissingComponent> missing = getMissingComponents();
        assertFalse(missing.isEmpty(), "Mora biti detektovano da nesto nedostaje");

        List<String> missingNames = missing.stream()
                .map(MissingComponent::getComponentName)
                .collect(Collectors.toList());

        assertTrue(missingNames.contains("HybridTurbine"),
            "HybridTurbine mora biti detektovana kao nedostajuca");
        assertTrue(missingNames.contains("Downpipe"),
            "Downpipe mora biti detektovan kao nedostajuci");
        assertTrue(missingNames.contains("Intercooler"),
            "Intercooler mora biti detektovan kao indirektan preduslov");

        System.out.println("Nedostajuce komponente za Stage2Tuning:");
        missing.forEach(m -> System.out.println("  - " + m.getComponentName()));
    }

    @Test
    public void test_Stage2_ImaHybridTurbine_NedostajeDownpipeIIntercooler() {
        // Vozilo ima HybridTurbine ali ne Downpipe ni Intercooler
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "HybridTurbine"));
        kieSession.insert(stage2Request());
        kieSession.fireAllRules();

        List<String> missingNames = getMissingComponents().stream()
                .map(MissingComponent::getComponentName)
                .collect(Collectors.toList());

        assertFalse(missingNames.contains("HybridTurbine"),
            "HybridTurbine postoji, ne sme biti u listi nedostajucih");
        assertTrue(missingNames.contains("Downpipe"),
            "Downpipe mora biti detektovan kao nedostajuci");
        assertTrue(missingNames.contains("Intercooler"),
            "Intercooler mora biti detektovan - preduslov za HybridTurbine");

        System.out.println("Nedostajuce komponente:");
        getMissingComponents().forEach(m -> System.out.println("  - " + m.getComponentName()));
    }

    @Test
    public void test_Stage2_ImaSveKomponente_NemaMissingComponent() {
        // Vozilo ima sve potrebne komponente -> nema MissingComponent
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "HybridTurbine"));
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "Downpipe"));
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "Intercooler"));
        kieSession.insert(stage2Request());
        kieSession.fireAllRules();

        List<MissingComponent> missing = getMissingComponents();
        assertTrue(missing.isEmpty(),
            "Vozilo ima sve komponente - ne sme biti MissingComponent");
        System.out.println("Vozilo ima sve komponente za Stage2Tuning - tuning dozvoljen.");
    }

    // ---------------------------------------------------------------
    // Testovi za HighPerformanceTuning (dublja hijerarhija)
    // ---------------------------------------------------------------

    @Test
    public void test_HighPerf_NemaKomponenti_DubokiLanac() {
        // HighPerf trazi Stage2Tuning koji trazi HybridTurbine + Downpipe + Intercooler
        // Query mora rekurzivno proci kroz ceo lanac
        kieSession.insert(highPerfRequest());
        kieSession.fireAllRules();

        List<String> missingNames = getMissingComponents().stream()
                .map(MissingComponent::getComponentName)
                .collect(Collectors.toList());

        // Direktni preduslovi za HighPerf
        assertTrue(missingNames.contains("Stage2Tuning"),
            "Stage2Tuning mora biti detektovan kao nedostajuci");
        assertTrue(missingNames.contains("Downpipe"),
            "Downpipe mora biti detektovan");
        // Indirektni preduslovi (kroz Stage2Tuning)
        assertTrue(missingNames.contains("HybridTurbine"),
            "HybridTurbine mora biti detektovana (indirektan preduslov)");
        assertTrue(missingNames.contains("Intercooler"),
            "Intercooler mora biti detektovan (indirektan preduslov)");

        System.out.println("Nedostajuce komponente za HighPerformanceTuning:");
        getMissingComponents().forEach(m -> System.out.println("  - " + m.getComponentName()));
    }

    @Test
    public void test_HighPerf_ImaSveKomponente_NemaMissingComponent() {
        // Vozilo ima sve sto treba za HighPerf
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "Stage2Tuning"));
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "Downpipe"));
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "HybridTurbine"));
        kieSession.insert(new VehicleHasComponent(VEHICLE_ID, "Intercooler"));
        kieSession.insert(highPerfRequest());
        kieSession.fireAllRules();

        assertTrue(getMissingComponents().isEmpty(),
            "Vozilo ima sve - ne sme biti MissingComponent");
        System.out.println("Vozilo ima sve komponente za HighPerformanceTuning.");
    }

    // ---------------------------------------------------------------
    // Edge case: Stage1 nema preduslova
    // ---------------------------------------------------------------

    @Test
    public void test_Stage1_NemaPreduslova_NemaMissingComponent() {
        // Stage1 nije definisan u hijerarhiji preduslova -> nema MissingComponent
        TuningRequest stage1 = new TuningRequest(VEHICLE_ID, EcuType.BOSCH_EDC17, "VW",
                1.7, 840.0, 390.0, "Stage1");
        kieSession.insert(stage1);
        kieSession.fireAllRules();

        assertTrue(getMissingComponents().isEmpty(),
            "Stage1 nema hardverskih preduslova - nema MissingComponent");
        System.out.println("Stage1 nema hardverskih preduslova - ispravno.");
    }
}