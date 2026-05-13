package com.ftn.sbnz.model.models;

import com.ftn.sbnz.model.enums.EcuType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Zahtev za tuning koji korisnik podnosi sistemu.
 * Template pravila proveravaju fizicke limite (maxBoost, maxTemp, maxTorque).
 * Accumulate pravila proveravaju stanje motora i blokiraju agresivne nivoe
 * ako je motor visoko istroson (HIGH_WEAR).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID vozila */
    private String vehicleId;

    /** Tip motornog racunara */
    private EcuType ecuType;

    /** Marka vozila (npr. "VW", "BMW") */
    private String brand;

    /** Trazeni pritisak turbine u barima (npr. 2.1) */
    private double requestedBoost;

    /** Trazena temperatura izduvnih gasova u °C (npr. 840) */
    private double requestedTemp;

    /** Trazeni obrtni moment u Nm (npr. 380) */
    private double requestedTorque;

    /**
     * Trazeni nivo tuninga.
     * Vrednosti: "Stage 1", "Stage 2", "Eco"
     * Korisnik bira nivo, a sistem proverava da li je bezbedno
     * primeniti ga na osnovu stanja motora.
     */
    private String tuningStage;
}