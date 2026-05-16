package com.ftn.sbnz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Global servis koji se ubacuje u Drools CEP sesiju.
 * Predstavlja kanal komunikacije izmedju Drools pravila i Spring Boot sloja.
 * Pravila pozivaju metode ovog servisa iz then bloka.
 *
 * Za potrebe projekta: biljezi sve notifikacije u listu
 * (vidljivo u testovima i na konzoli).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<String> notifications = new ArrayList<>();

    public void sendHighTempWarning(String vehicleId, double measuredTemp, double limitTemp) {
        String msg = String.format(
            "[CEP WARNING] Vozilo %s: Temperatura izduvnih gasova %.1f°C prelazi limit %.1f°C",
            vehicleId, measuredTemp, limitTemp
        );
        notifications.add(msg);
        log.warn(msg);
    }

    public void sendThermalOverloadAlert(String vehicleId) {
        String msg = String.format(
            "[CEP CRITICAL] Vozilo %s: ThermalOverloadEvent - temperatura prekoracila limit " +
            "vise od 3 puta u 20 sekundi! Aktiviran Safe Mode - pritisak turbine se obara.",
            vehicleId
        );
        notifications.add(msg);
        log.error(msg);
    }

    public void sendOilPressureCritical(String vehicleId, double pressure, int rpm) {
        String msg = String.format(
            "[CEP CRITICAL] Vozilo %s: Kriticno nizak pritisak ulja %.2f bara pri %d RPM " +
            "u kontinuitetu 3 sekunde! EngineCriticalEvent aktiviran.",
            vehicleId, pressure, rpm
        );
        notifications.add(msg);
        log.error(msg);
    }

    public void sendSafeModeActivated(String vehicleId, String reason) {
        String msg = String.format(
            "[CEP SAFE MODE] Vozilo %s: %s", vehicleId, reason
        );
        notifications.add(msg);
        log.error(msg);
    }

    public List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public void clearNotifications() {
        notifications.clear();
    }
}