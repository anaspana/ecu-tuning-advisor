package com.ftn.sbnz.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Role;

import java.io.Serializable;

// Intermedijarni dogadjaj - okida se kada jedna ExhaustTempEvent
// predje maxTemp limit; koristi se za brojanje u sliding window-u
@Role(Role.Type.EVENT)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighTempWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private double measuredTemp;
    private double limitTemp;
}