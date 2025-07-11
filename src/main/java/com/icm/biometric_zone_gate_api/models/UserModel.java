package com.icm.biometric_zone_gate_api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id; // userId
    private String name;
    private Integer privilege; // 0: usuario, 1: admin
    private String cardNumber;
    private String password;
    private LocalDate vaildStart;
    private LocalDate vaildEnd;

    @Lob
    private String photoBase64;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FingerprintModel> fingerprints;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private FaceModel faceModel;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private PalmModel palmModel;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<TimeGroupModel> timeGroupsModels;
}
