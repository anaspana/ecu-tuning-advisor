package com.ftn.sbnz.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

import java.io.Serializable;

// Dogadjaj sa senzora pritiska ulja u motoru
// Kritican parametar - nizak pritisak ulja pri visokim obrtajima
// moze dovesti do trajnog ostecenja motora
@Role(Role.Type.EVENT)
@Timestamp("timestamp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OilPressureEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;

    // Pritisak ulja u barima
    private double pressureBar;

    // Broj obrtaja motora u trenutku ocitavanja
    private int rpm;

    // Unix timestamp ocitavanja u milisekundama
    private long timestamp;
}