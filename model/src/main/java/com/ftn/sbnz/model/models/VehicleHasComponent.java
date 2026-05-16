package com.ftn.sbnz.model.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Komponenta koju vozilo fizički poseduje.
// Sistem proverava da li su sve HardwarePrerequisite
// pokriveene ovim objektima pre nego što odobri tuning.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHasComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vehicleId;
    private String componentName;
}