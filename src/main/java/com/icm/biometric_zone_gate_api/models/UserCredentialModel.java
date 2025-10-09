package com.icm.biometric_zone_gate_api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_credentials")
public class UserCredentialModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 0–9 = fingerprint, 10 = password, 11 = card
    @Column(nullable = false)
    private Integer backupNum;

    // según doc: <1620 para THbio3.0
    @Column(length = 1600)
    private String record;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserModel user;
}
