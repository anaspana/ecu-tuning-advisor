package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Cinjenica koju pravilo ubacuje u radnu memoriju kada blokira
// zahtev za tuning zbog loseg stanja motora (HighWear).
// Konzistentno sa pattern-om Forward Chaining-a - rezultat je
// nova cinjenica, ne modifikacija ulaznog objekta.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningBlockedDecision implements Serializable {

    private static final long serialVersionUID = 1L;

    // ID vozila
    private String vehicleId;

    // Trazeni nivo tuninga koji je blokiran (npr. "Stage1")
    private String blockedStage;

    // Razlog blokade
    private String reason;
}