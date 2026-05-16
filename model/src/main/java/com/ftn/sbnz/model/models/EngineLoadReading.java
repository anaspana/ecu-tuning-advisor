package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

// Jedno historijsko ocitavanje opterecenja motora iz prethodnih voznji
// Accumulate pravila agregiraju ova ocitavanja po vehicleId
// i racunaju prosecno opterecenje motora
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineLoadReading implements Serializable {

    private static final long serialVersionUID = 1L;

    // ID vozila
    private String vehicleId;

    // Opterecenje motora u procentima (0.0 - 100.0)
    // Vrednost >75% u proseku ukazuje na agresivnu voznju / visoko habanje
    private double engineLoadPercent;

    // Datum voznje (radi sledljivosti)
    private LocalDate readingDate;
}