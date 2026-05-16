package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Cinjenica koju pravilo insertuje kada otkrije da vozilu
// nedostaje hardverski preduslov za trazeni nivo tuninga
// Sistem tacno prijavljuje koja komponenta nedostaje
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissingComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private String componentName;
    private String requiredForStage;
}