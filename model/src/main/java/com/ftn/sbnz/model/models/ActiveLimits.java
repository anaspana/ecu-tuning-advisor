package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Aktivni limiti za konkretno vozilo, kreirani od strane Java servisa
// na osnovu TuningDecision (koji dolazi iz Template mehanizma).
// Insertuje se u CEP sesiju kao referentna tacka za alarme.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveLimits implements Serializable {

    private static final long serialVersionUID = 1L;

    // ID vozila za koje vaze ovi limiti
    private String vehicleId;

    // Maksimalna dozvoljena temperatura izduvnih gasova u °C
    private double maxTemp;

    // Maksimalni dozvoljeni pritisak turbine u barima
    private double maxBoost;

    // Maksimalni dozvoljeni obrtni moment u Nm
    private double maxTorque;
}