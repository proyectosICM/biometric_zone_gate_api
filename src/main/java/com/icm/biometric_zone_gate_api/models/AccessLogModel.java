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
@Table(name = "acess_log")
public class AccessLogModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private String userId;
    private String verifyMode;
    private int ioMode;
    private String inOut;
    private String doorMode;

    @Lob
    private String logPhoto;

    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private DeviceModel device;
}
