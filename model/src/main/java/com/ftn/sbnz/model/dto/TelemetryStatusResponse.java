package com.ftn.sbnz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//DTO koji servis vraca na GET /api/telemetry/status/{vehicleId}.
// Sadrzi trenutno stanje CEP sesije: alarme, safe mode flag i broj upozorenja
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryStatusResponse {

    // ID vozila
    private String vehicleId;

    // Da li je Safe Mode trenutno aktivan
    // True znaci da je sistem detektovao kriticno preopterecenje
    // i softverski oborio pritisak turbine
    private boolean safeModeActive;

    // Broj HighTempWarning dogadjaja od poslednjeg reseta
    private int highTempWarningCount;

    // Da li je EngineCriticalEvent (pritisak ulja) bio aktivan
    private boolean oilPressureCritical;

    // Sve notifikacije iz NotificationService za ovo vozilo,
    // hronoloski od najstarijeg ka najnovijem
    private List<String> notifications;
}