package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Cinjenica koju accumulate pravilo ubacuje u radnu memoriju
// nakon analize istorijskih ocitavanja opterecenja motora
// Koristi se u pravilima za blokadu agresivnog tuninga
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineWearStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum WearLevel { HIGH_WEAR, NORMAL_WEAR }

    // ID vozila
    private String vehicleId;

    // Nivo habanja motora
    private WearLevel wearLevel;

    // Izracunati prosek opterecenja (cuvamo radi logovanja/prikaza)
    private double averageLoadPercent;
}