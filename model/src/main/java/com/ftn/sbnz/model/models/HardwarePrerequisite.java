package com.ftn.sbnz.model.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.kie.api.definition.type.Position;

import java.io.Serializable;

// Predstavlja hardverski preduslov za određeni nivo tuninga
// Npr: HybridTurbine je potrebna za Stage2Tuning

// @Position anotacije su obavezne za Drools backward chaining query
// sa pozicionim argumentima: HardwarePrerequisite(x, y;)
@Data
@NoArgsConstructor
public class HardwarePrerequisite implements Serializable {

    private static final long serialVersionUID = 1L;

    @Position(0)
    private String component;

    @Position(1)
    private String requiredFor;

    public HardwarePrerequisite(String component, String requiredFor) {
        this.component = component;
        this.requiredFor = requiredFor;
    }
}