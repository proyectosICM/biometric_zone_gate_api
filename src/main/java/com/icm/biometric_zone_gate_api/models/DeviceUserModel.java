package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Representa el usuario específico dentro de un dispositivo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_user")
public class DeviceUserModel {
    /** ELIMINAR ESTA TABLA */
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enroll_id", nullable = false)
    private Integer enrollId;

    // 0 = normal, 1 = admin
    private Integer adminLevel = 0;

    private Boolean synced = true;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
