package com.ftn.sbnz.model.dto;

import com.ftn.sbnz.model.enums.EcuType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO koji frontend/klijent salje na REST endpoint.
 * Sadrzi sve podatke o vozilu potrebne za dijagnostiku.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequest {

    private String vehicleId;

    // Tip ECU racunara
    private EcuType ecuType;

    // Marka vozila
    private String vehicleBrand;

    // Predjena kilometraza
    private double mileage;

    // Maksimalni obrtni moment menjaca (Nm)
    private double maxGearboxTorque;

    // Lista DTC kodova koje vozilo prijavljuje
    private List<String> dtcCodes;
}