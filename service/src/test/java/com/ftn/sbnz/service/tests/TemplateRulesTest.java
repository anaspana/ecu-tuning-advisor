package com.ftn.sbnz.service.tests;

import com.ftn.sbnz.model.enums.EcuType;
import com.ftn.sbnz.model.models.TuningDecision;
import com.ftn.sbnz.model.models.TuningRequest;
import com.ftn.sbnz.service.TemplateService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

// Testovi za Template mehanizam - provera ECU limita iz CSV tabele.
//
// ecu-limits.csv:
//   BOSCH_EDC17 / VW      : boost 1.8, temp 850, torque 400
//   BOSCH_EDC17 / BMW     : boost 2.0, temp 870, torque 450
//   BOSCH_EDC17 / Audi    : boost 1.9, temp 860, torque 420
//   BOSCH_EDC16 / VW      : boost 1.5, temp 800, torque 350
//   BOSCH_EDC16 / BMW     : boost 1.6, temp 810, torque 370
//   SIEMENS_SID807/Peugeot: boost 1.6, temp 820, torque 300
//   SIEMENS_SID807/Ford   : boost 1.55,temp 815, torque 310
public class TemplateRulesTest {

    private final TemplateService service = new TemplateService();

    @BeforeEach
    public void ispisImenaTesta(TestInfo testInfo) {
        System.out.println("\n===============================================================");
        System.out.println("TEST: " + testInfo.getDisplayName());
        System.out.println("===============================================================");
    }

    // ---------------------------------------------------------------
    // BLOCK testovi - prekoracenje barem jednog parametra
    // ---------------------------------------------------------------

    @Test
    public void test_EDC17_VW_BlockByBoost() {
        // Boost 2.1 > limit 1.8 -> BLOCK
        TuningRequest req = new TuningRequest("VW-001",
                EcuType.BOSCH_EDC17, "VW", 2.1, 840.0, 380.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }

    @Test
    public void test_EDC17_VW_BlockByTorque() {
        // Torque 410 > limit 400 -> BLOCK
        TuningRequest req = new TuningRequest("VW-002",
                EcuType.BOSCH_EDC17, "VW", 1.7, 840.0, 410.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }

    @Test
    public void test_EDC17_VW_BlockByTemp() {
        // Temp 860 > limit 850 -> BLOCK
        TuningRequest req = new TuningRequest("VW-003",
                EcuType.BOSCH_EDC17, "VW", 1.7, 860.0, 380.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }

    @Test
    public void test_EDC17_BMW_BlockByBoostAndTorque() {
        // Boost 2.5 > 2.0 i Torque 460 > 450 -> BLOCK, oba razloga
        TuningRequest req = new TuningRequest("BMW-001",
                EcuType.BOSCH_EDC17, "BMW", 2.5, 860.0, 460.0, "Stage 2");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }

    @Test
    public void test_SID807_Peugeot_Block() {
        // Boost 1.7 > 1.6 -> BLOCK
        TuningRequest req = new TuningRequest("PEU-001",
                EcuType.SIEMENS_SID807, "Peugeot", 1.7, 810.0, 290.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }

    // ---------------------------------------------------------------
    // ALLOW testovi - svi parametri unutar limita
    // ---------------------------------------------------------------

    @Test
    public void test_EDC17_VW_Allow() {
        // sve ispod limita: boost 1.7, temp 840, torque 390
        TuningRequest req = new TuningRequest("VW-010",
                EcuType.BOSCH_EDC17, "VW", 1.7, 840.0, 390.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.ALLOW, decision.getDecision());
    }

    @Test
    public void test_EDC17_BMW_Allow() {
        // Boost 2.0 == limit, Temp 870 == limit, Torque 450 == limit -> ALLOW (granicne vrednosti)
        TuningRequest req = new TuningRequest("BMW-010",
                EcuType.BOSCH_EDC17, "BMW", 2.0, 870.0, 450.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.ALLOW, decision.getDecision());
    }

    @Test
    public void test_EDC16_VW_Allow() {
        // Boost 1.4, temp 780, torque 340 -> ALLOW
        TuningRequest req = new TuningRequest("VW-020",
                EcuType.BOSCH_EDC16, "VW", 1.4, 780.0, 340.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.ALLOW, decision.getDecision());
    }

    @Test
    public void test_SID807_Ford_Allow() {
        // Boost 1.5, temp 810, torque 300 -> ALLOW (sve <= limiti: 1.55, 815, 310)
        TuningRequest req = new TuningRequest("FORD-001",
                EcuType.SIEMENS_SID807, "Ford", 1.5, 810.0, 300.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.ALLOW, decision.getDecision());
    }

    // ---------------------------------------------------------------
    // Edge case - nepostojeca kombinacija ECU/brand
    // ---------------------------------------------------------------

    @Test
    public void test_UnknownCombination_ReturnsBlock() {
        // Toyota nije u CSV tabeli -> BLOCK sa porukom o nepostojanju limita
        TuningRequest req = new TuningRequest("UNKNOWN-001",
                EcuType.BOSCH_EDC17, "Toyota", 1.5, 800.0, 350.0, "Stage1");
        TuningDecision decision = service.evaluateTuning(req);

        assertNotNull(decision);
        assertEquals(TuningDecision.Decision.BLOCK, decision.getDecision());
    }
}
