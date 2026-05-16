package com.ftn.sbnz.model.dto;

import com.ftn.sbnz.model.models.MissingComponent;
import com.ftn.sbnz.model.models.TuningDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO koji servis vraca nakon kombinovane Template + Backward Chaining validacije
// Sadrzi sve razloge zbog kojih je tuning dozvoljen ili blokiran
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningValidateResponse {

    // ID vozila
    private String vehicleId;

    // Finalna odluka: ALLOW samo ako su i Template i Backward Chaining OK
    //BLOCK ako bilo koji od mehanizama blokira
    private TuningDecision.Decision finalDecision;

    // Odluka Template pravila (fizicki limiti ECU-a)
    private TuningDecision templateDecision;

    // Lista hardverskih komponenti koje nedostaju vozilu
    // za trazeni nivo tuninga (iz Backward Chaining).
    // Prazna lista znaci da su svi hardverski preduslovi ispunjeni.
    private List<MissingComponent> missingComponents;

    // Sve poruke o razlozima blokade ili odobrenja
    private List<String> messages;
}