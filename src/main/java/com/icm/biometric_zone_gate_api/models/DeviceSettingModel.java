package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "device_settings")
public class DeviceSettingModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    private String deviceName;
    private String serverHost;
    private Integer serverPort;
    private String idioma;
    private Integer volumen;
    private Boolean antiPass;
    private Integer doorOpenDelay;
    private String verificationMode;
    private String oldPwd; // Puede omitirse si no guardas claves
    private String newPwd; // Puede omitirse si no guardas claves

    @OneToOne(mappedBy = "setting")
    @JsonIgnore
    private DeviceModel device;
}
