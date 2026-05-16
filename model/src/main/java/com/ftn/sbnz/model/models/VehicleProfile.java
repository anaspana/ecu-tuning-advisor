package com.ftn.sbnz.model.models;

import com.ftn.sbnz.model.enums.EcuType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Profil vozila - nosi sve relevantne podatke o vozilu
 * i fabricke limite za odabrani ECU tip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Jedinstveni identifikator vozila */
    private String vehicleId;

    /** Tip motornog racunara (npr. Bosch EDC17) */
    private EcuType ecuType;

    /** Marka vozila (npr. "BMW", "Volkswagen") */
    private String vehicleBrand;

    /** Predjena kilometraza */
    private double mileage;

    /** Maksimalni obrtni moment koji menjac moze da izdrzi (Nm) */
    private double maxGearboxTorque;

    /** Maksimalna dozvoljena temperatura izduvnih gasova (°C) */
    private double maxExhaustTemp;

    /** Maksimalni bezbedni pritisak turbine (bar) */
    private double maxBoostPressure;
}