package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "devices")
public class DeviceModel {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sn; // serial number

    private String name;

    private String host; // ip or host

    private String port; // port conection

    // NOTE: Change to Enum
    // Language code (0=English, 9=Spanish, etc.)
    private int language;

    // Volume level (0–10)
    private int volume;

    // Anti-passback mode (0=disabled, 1=host inside, 2=host outside)
    private int antiPassback;

    // Sleep enabled (true=sleep mode active)
    private boolean sleepEnabled;

    // NOTE: Change to ENUM
    /**
     * Verification mode (0–4)
     * 0: FP or Card or Pwd
     * 1: Card + FP
     * 2: Pwd + FP
     * 3: Card + FP + Pwd
     * 4: Card + Pwd
     */
    private int verificationMode;

    // Number of fingerprints per user (1–10, default 3)
    private int userFpNum;

    // Reverify time (0–255 minutes)
    private int reverifyTime;

    // Log hint threshold (when remaining logs < loghint, device warns)
    private int logHint;

    // NOTE: revisar
    // Push enabled (custom, not from protocol)
    private boolean pushEnabled;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyModel company;

    @JsonIgnore
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<DeviceUserAccessModel> deviceUsers = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
