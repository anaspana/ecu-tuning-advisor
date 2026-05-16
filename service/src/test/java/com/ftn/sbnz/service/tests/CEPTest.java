package com.ftn.sbnz.service.tests;

import com.ftn.sbnz.model.events.*;
import com.ftn.sbnz.model.models.ActiveLimits;
import com.ftn.sbnz.model.models.EngineStatus;
import com.ftn.sbnz.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.time.SessionPseudoClock;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// CEP testovi - Pseudo clock za preciznu kontrolu vremena
// Limiti za EDC17/VW iz template tabele: 
// maxTemp=850°C, maxBoost=1.8 bar, maxTorque=400 Nm
// Scenario 1: >3x prekoracenja temperature u 20s -> SAFE_MODE
// Scenario 2: pritisak ulja <1.2 bara pri RPM>2500 u kontinuitetu 3s -> CRITICAL
public class CEPTest {

    private KieSession kieSession;
    private SessionPseudoClock clock;
    private NotificationService notificationService;

    // Limiti za EDC17/VW iz template tabele
    private static final String VEHICLE_ID = "VW-CEP-001";
    private static final double MAX_TEMP = 850.0;
    private static final double MAX_BOOST = 1.8;
    private static final double MAX_TORQUE = 400.0;

    @BeforeEach
    public void setup(TestInfo testInfo) {
        System.out.println("\n===============================================================");
        System.out.println("TEST: " + testInfo.getDisplayName());
        System.out.println("===============================================================");

        KieServices ks = KieServices.Factory.get();
        KieContainer kc = ks.getKieClasspathContainer();
        kieSession = kc.newKieSession("cepKsession");
        clock = kieSession.getSessionClock();
        notificationService = new NotificationService();
        kieSession.setGlobal("notificationService", notificationService);

        // Insertuj ActiveLimits u sesiju (kao sto bi to uradio CEPService)
        kieSession.insert(new ActiveLimits(VEHICLE_ID, MAX_TEMP, MAX_BOOST, MAX_TORQUE));
    }

    @AfterEach
    public void teardown() {
        if (kieSession != null) kieSession.dispose();
        notificationService.clearNotifications();
    }

    // pomocna metoda
    private void insertTempEvent(double temp, long advanceMs) {
        clock.advanceTime(advanceMs, TimeUnit.MILLISECONDS);
        kieSession.insert(new ExhaustTempEvent(VEHICLE_ID, temp, clock.getCurrentTime()));
        kieSession.fireAllRules();
    }

    // pomocna metoda za oil test
    private void insertOilEvent(double pressure, int rpm, long advanceMs) {
        clock.advanceTime(advanceMs, TimeUnit.MILLISECONDS);
        kieSession.insert(new OilPressureEvent(VEHICLE_ID, pressure, rpm, clock.getCurrentTime()));
        kieSession.fireAllRules();
    }

    private Optional<EngineStatus> getEngineStatus() {
        return kieSession.getObjects().stream()
                .filter(f -> f instanceof EngineStatus)
                .map(f -> (EngineStatus) f)
                .filter(s -> s.getVehicleId().equals(VEHICLE_ID))
                .findFirst();
    }

    // ---------------------------------------------------------------
    // SCENARIO 1: Temperatura - sliding window 20s
    // ---------------------------------------------------------------

    @Test
    public void test_Temp_JednoPrekoracenje_NemaSafeMode() {
        // Samo 1 prekoracenje -> nema SafeMode (treba > 3)
        insertTempEvent(860.0, 1000); // prekoracuje 850

        assertFalse(getEngineStatus().isPresent(),
            "Jedno prekoracenje ne sme aktivirati Safe Mode");

        long warnings = kieSession.getObjects().stream()
                .filter(f -> f instanceof HighTempWarning).count();
        assertEquals(1, warnings, "Mora biti tacno 1 HighTempWarning");
        System.out.println("1 prekoracenje -> HighTempWarning bez SafeMode");
    }

    @Test
    public void test_Temp_TacnoTriPrekoracenja_NemaSafeMode() {
        // Tacno 3 prekoracenja u 20s -> nema SafeMode (uslov je VISE od 3)
        insertTempEvent(860.0, 1000);
        insertTempEvent(870.0, 3000);
        insertTempEvent(880.0, 5000);

        assertFalse(getEngineStatus().isPresent(),
            "Tacno 3 prekoracenja ne sme aktivirati Safe Mode - uslov je vise od 3");
        System.out.println("3 prekoracenja -> nema SafeMode (granicna vrednost)");
    }

    @Test
    public void test_Temp_CetiriPrekoracenjaU20s_SafeMode() {
        // 4 prekoracenja u 20s -> SAFE_MODE
        insertTempEvent(860.0, 1000);   // t=1s
        insertTempEvent(870.0, 4000);   // t=5s
        insertTempEvent(880.0, 4000);   // t=9s
        insertTempEvent(890.0, 4000);   // t=13s - cetvrto, aktivira SafeMode

        Optional<EngineStatus> status = getEngineStatus();
        assertTrue(status.isPresent(), "Safe Mode mora biti aktiviran");
        assertEquals(EngineStatus.Status.SAFE_MODE, status.get().getStatus());

        boolean thermalOverload = kieSession.getObjects().stream()
                .anyMatch(f -> f instanceof ThermalOverloadEvent);
        assertTrue(thermalOverload, "ThermalOverloadEvent mora biti insertovan");

        assertFalse(notificationService.getNotifications().isEmpty(),
            "NotificationService mora imati poruke");
        notificationService.getNotifications().forEach(System.out::println);
    }

    @Test
    public void test_Temp_PrekoracenjaVanProzora_NemaSafeMode() {
        // 4 prekoracenja ali razmazena > 20s -> nema SafeMode
        // u svakom prozoru od 20s ima <= 3
        insertTempEvent(860.0, 1000);    // t=1s
        insertTempEvent(870.0, 7000);    // t=8s
        insertTempEvent(880.0, 7000);    // t=15s
        // Do ovde: 3 u prozoru [0,20s]
        insertTempEvent(890.0, 10000);   // t=25s - prvi ispao iz prozora, opet 3 u prozoru [5,25]
        // Cetvrto u istom 20s prozoru? Ne - 1s je ispalo

        assertFalse(getEngineStatus().isPresent(),
            "Prekoracenja van 20s prozora ne smeju aktivirati SafeMode");
        System.out.println("Prekoracenja van prozora od 20s -> nema SafeMode");
    }

    @Test
    public void test_Temp_NormalnaTemperatura_NemaUpozorenja() {
        // Temperatura ispod limita -> nema nikakvih dogadjaja
        insertTempEvent(840.0, 1000);
        insertTempEvent(845.0, 3000);
        insertTempEvent(849.9, 3000);

        assertTrue(kieSession.getObjects().stream()
                .noneMatch(f -> f instanceof HighTempWarning),
            "Normalna temperatura ne sme generisati upozorenja");
        System.out.println("Normalna temperatura -> nema upozorenja");
    }

    @Test
    public void test_Temp_SafeModeSeNePonavljaZaIstoVozilo() {
        // SafeMode aktiviran -> drugi ThermalOverload ne sme kreirati jos jedan EngineStatus
        insertTempEvent(860.0, 1000);
        insertTempEvent(870.0, 2000);
        insertTempEvent(880.0, 2000);
        insertTempEvent(890.0, 2000);  // aktivira SafeMode
        insertTempEvent(900.0, 1000);  // jos jedno prekoracenje
        insertTempEvent(910.0, 1000);

        long statusCount = kieSession.getObjects().stream()
                .filter(f -> f instanceof EngineStatus).count();
        assertEquals(1, statusCount, "SafeMode ne sme biti aktiviran vise puta");
        System.out.println("SafeMode se ne ponavlja");
    }

    // ---------------------------------------------------------------
    // SCENARIO 2: Pritisak ulja - kontinuitet 3 sekunde
    // ---------------------------------------------------------------

    @Test
    public void test_Oil_KriticniPritisak3Sekunde_Critical() {
        // Kriticni pritisak u rasponu od pune 3 sekunde -> CRITICAL
        // t=1s (prvi dogadjaj)
        insertOilEvent(1.0, 3000, 1000);   
        // t=2s (razlika 1s)
        insertOilEvent(0.9, 3000, 1000);   
        // t=3s (razlika 2s)
        insertOilEvent(0.8, 3000, 1000);   
        // t=4s (razlika 3s od prvog - ovo okida pravilo)
        insertOilEvent(0.7, 3000, 1000);

        Optional<EngineStatus> status = getEngineStatus();
        assertTrue(status.isPresent(), "EngineCritical mora biti aktiviran");
        assertEquals(EngineStatus.Status.CRITICAL, status.get().getStatus());

        boolean criticalEvent = kieSession.getObjects().stream()
                .anyMatch(f -> f instanceof EngineCriticalEvent);
        assertTrue(criticalEvent, "EngineCriticalEvent mora biti insertovan");

        notificationService.getNotifications().forEach(System.out::println);
    }

    @Test
    public void test_Oil_NormalanPritisak_NemaCritical() {
        // Pritisak iznad 1.2 -> nema Critical
        insertOilEvent(1.5, 2000, 1000);
        insertOilEvent(1.8, 2000, 1000);
        insertOilEvent(2.0, 2000, 1000);

        assertFalse(getEngineStatus().isPresent(),
            "Normalan pritisak ulja ne sme aktivirati Critical");
        System.out.println("Normalan pritisak -> nema Critical");
    }

    @Test
    public void test_Oil_NiskiPritisak_NiskiRPM_NemaCritical() {
        // Nizak pritisak ali RPM <= 2500 -> nema Critical (uslov je RPM > 2500)
        insertOilEvent(0.8, 1000, 2000); // rpm=2000
        insertOilEvent(0.9, 1000, 2000);
        insertOilEvent(0.7, 1000, 2000);
        insertOilEvent(0.6, 2000, 1000);

        assertFalse(getEngineStatus().isPresent(),
            "Nizak pritisak pri niskim RPM ne sme aktivirati Critical");
        System.out.println("Nizak pritisak pri niskim RPM -> nema Critical");
    }

    @Test
    public void test_Oil_KriticniPritisak_KratakInterval_NemaCritical() {
        // Kriticni pritisak ali samo 2 sekunde -> nema Critical (treba 3s)
        insertOilEvent(1.0, 3000, 1000);   // t=1s
        insertOilEvent(0.9, 3000, 1000);   // t=2s
        // nema treceg

        assertFalse(getEngineStatus().isPresent(),
            "Manje od 3 sekunde kriticnog pritiska ne sme aktivirati Critical");
        System.out.println("Kratkotrajna kritika -> nema Critical");
    }
}