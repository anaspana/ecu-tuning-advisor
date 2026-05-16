package com.ftn.sbnz.model.dto;

import com.ftn.sbnz.model.enums.EcuType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO za endpoint POST /api/tuning/validate
// Pokrece Template pravila (fizicki limiti) i Backward Chaining
// (hardverski preduslovi) u jednoj akciji
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningValidateRequest {

    // ID vozila
    private String vehicleId;

    // Tip motornog racunara
    private EcuType ecuType;

    // Marka vozila (npr. "VW", "BMW")
    private String brand;

    //Trazeni nivo tuninga.
    // Vrednosti: "Stage1", "Stage2", "Eco", "Stage2Tuning", "HighPerformanceTuning"
    private String tuningStage;

    // Trazeni pritisak turbine u barima
    private double requestedBoost;

    // Trazena temperatura izduvnih gasova u °C
    private double requestedTemp;

    // Trazeni obrtni moment u Nm
    private double requestedTorque;

    // Lista komponenti koje vozilo ima ugradene
    // Backward Chaining proverava da li su zadovoljeni svi hardverski preduslovi
    // npr. ["HybridTurbine", "Downpipe", "Intercooler"]
    private List<String> vehicleComponents;
}