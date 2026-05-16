package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Cinjenica koja predstavlja trenutni status motora u radnoj memoriji
// Insertuje se kada se aktivira Safe mode ili kriticni dogadjaj
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { NORMAL, SAFE_MODE, CRITICAL }

    private String vehicleId;
    private Status status;
    private String reason;
}