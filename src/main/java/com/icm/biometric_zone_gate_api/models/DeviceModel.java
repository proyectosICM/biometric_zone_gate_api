package com.icm.biometric_zone_gate_api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "devices")
public class DeviceModel {
    @Id
    @Column(name = "id", nullable = false)
    private String id;
    private String model;
    private String firmwareVersion;
    private String name;
    private String token;
    private Boolean activo;
    private LocalDateTime lastSeen; // última vez que se comunicó

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "setting_id", referencedColumnName = "id")
    private DeviceSettingModel setting;
}
