package com.icm.biometric_zone_gate_api.models;

import com.icm.biometric_zone_gate_api.enums.CredentialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_credentials")
public class UserCredentialModel {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 0–9 = fingerprint, 10 = password, 11 = card
    @Column(nullable = false)
    private Integer backupNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialType type;

    // según doc: <1620 para THbio3.0
    @Column(length = 1600)
    private String record;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_user_id", referencedColumnName = "id", nullable = false)
    private DeviceUserModel deviceUser;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
