package com.ftn.sbnz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO za endpoint POST /api/diagnosis/history.
 * Sadrzi listu istorijskih ocitavanja opterecenja motora
 * na osnovu kojih Accumulate pravila racunaju prosecno habanje.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccumulateRequest {

    // ID vozila
    private String vehicleId;
    
    // Lista ocitavanja opterecenja motora iz prethodnih voznji (0.0 - 100.0).
    // Minimalno 1, preporuceno 5-30 ocitavanja za pouzdanu analizu.
    private List<Double> engineLoadHistory;
}