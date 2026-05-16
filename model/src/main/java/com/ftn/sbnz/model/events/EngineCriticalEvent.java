package com.ftn.sbnz.model.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Role;

import java.io.Serializable;

// kriticni dogadjaj - okida se kada je pritisak ulja ispod 1.2 bara
// pri RPM > 2500 u kontinuitetu od 3 sekunde
@Role(Role.Type.EVENT)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineCriticalEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private String message;
}