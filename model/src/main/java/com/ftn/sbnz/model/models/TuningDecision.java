package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Odluka sistema o zahtevu za tuning - ALLOW ili BLOCK.
 * Ubacuju je template pravila u radnu memoriju.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningDecision implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Decision { ALLOW, BLOCK }

    /** ID vozila */
    private String vehicleId;

    /** ALLOW ili BLOCK */
    private Decision decision;

    /** Razlog odluke (koji parametar je prekoracen, ili da su svi OK) */
    private String reason;

    /** ECU tip i marka za koji je pravilo okidano */
    private String profileKey;
}
