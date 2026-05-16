package com.ftn.sbnz.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Role;
import org.kie.api.definition.type.Timestamp;

import java.io.Serializable;

// Dogadjaj sa senzora temperature izduvnih gasova (EGT)
// Stize kao kontinuirani tok podataka tokom voznje
// @Role(Role.Type.EVENT) govori Drools-u da tretira ovu klasu kao CEP dogadjaj
@Role(Role.Type.EVENT)
@Timestamp("timestamp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExhaustTempEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;

    // Izmerena temperatura u °C
    private double temperature;

    // Unix timestamp ocitavanja u milisekundama
    private long timestamp;
}