package com.ftn.sbnz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO za endpoint POST /api/telemetry/event
//
// Predstavlja jedno ocitavanje sa senzora vozila u realnom vremenu
// Svaki poziv insertuje novi dogadjaj u CEP sesiju i moze okinuti pravila
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelemetryEventRequest {

    // ID vozila
    private String vehicleId;

    // Temperatura izduvnih gasova u °C
    // CEP prati da li prelazi max limit iz ActiveLimits za dati ECU
    // Null = senzor nije aktivan u ovom ocitavanju
    private Double exhaustTempCelsius;

    // Pritisak ulja u barima
    // CEP prati kriticno nizak pritisak (< 1.2 bara) pri visokim obrtajima
    // Null = senzor nije aktivan u ovom ocitavanju
    private Double oilPressureBar;

    // Broj obrtaja motora (RPM).
    // Koristi se zajedno sa oilPressureBar za detekciju kriticnog stanja
    private Integer rpm;
}