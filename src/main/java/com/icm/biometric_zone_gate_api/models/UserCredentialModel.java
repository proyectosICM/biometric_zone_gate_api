package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@Entity
@Table(name = "user_credentials")
public class UserCredentialModel {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 0–9 = fingerprint, 10 = password, 11 = card
    @Column(nullable = false)
    private Integer backupNum = 11;

    // según doc: <1620 para THbio3.0 y 65535 para fotos
    @Column(length = 65535)
    private String record = "1111";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    //@JsonBackReference("user-credentials")
    private UserModel user;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
