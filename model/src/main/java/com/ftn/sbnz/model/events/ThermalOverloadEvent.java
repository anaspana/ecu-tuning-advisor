package com.ftn.sbnz.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Role;

import java.io.Serializable;

// Kriticni dogadjaj - okida se kada se HighTempWarning pojavi
// vise od 3 puta u prozoru od 20 sekundi
// Aktivira Safe mode
@Role(Role.Type.EVENT)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThermalOverloadEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private String message;
}