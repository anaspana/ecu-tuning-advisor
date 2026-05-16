package com.ftn.sbnz.model.dto;

import com.ftn.sbnz.model.models.EngineWearStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO koji servis vraca nakon Accumulate analize istorije voznji.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccumulateResponse {

    // ID vozila
    private String vehicleId;

    // Izracunato prosecno opterecenje motora (0.0 - 100.0)
    private double averageLoadPercent;

    // HIGH_WEAR ako je prosek >75%, inace NORMAL_WEAR
    private EngineWearStatus.WearLevel wearLevel;

    // Da li je agresivni tuning (Stage1/Stage2) blokiran zbog habanja
    private boolean tuningBlocked;

    // Poruka korisniku
    private String message;
}