package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_user_access")
public class DeviceUserAccessModel {

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario registrado en el dispositivo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    // Dispositivo donde está registrado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceModel device;

    // weekzone del protocolo (zona horaria semanal)
    private Integer weekZone;

    // grupo de acceso (0=no grupo, 1~9)
    private Integer groupNumber;

    // vigencia de acceso
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    private Boolean enabled = true;
}
