package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.icm.biometric_zone_gate_api.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private boolean pendingClean = false;

    private ZonedDateTime lastUserSync;

    private String sn;

    private int volume;

    private int language;

    private String modelname;

    //how many fingerprints per user 1~10，default:3
    private int userfpnum;

    // User capacity 1000/3000/5000
    private Integer usersize;

    // Fingerprint capacity 1000/3000/5000
    private Integer fpsize;

    // Rfid card capacity 1000/3000/5000/10000
    private Integer cardsize;

    // Password capacity
    private Integer pwdsize;

    // Logs capacity
    private Integer logsize;

    private Integer useduser;

    private Integer usedfp;

    private Integer usedcard;

    private Integer usedpwd;

    private Integer usedlog;

    private Integer usednewlog;

    // Fingerprint algorithm thbio1.0 or thbio3.0
    private String fpalgo;

    // Terminal firmware
    private String firmware;

    // Terminal datetime
    private LocalDateTime time;

    /***********************/
    /*  WEB SYSTEM  */
    /***********************/

    // Device name in the web system
    private String name;

    // Device status in the web system¡
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.DISCONNECTED;

    // Relations DB
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyModel company;

    @JsonIgnore
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<DeviceUserAccessModel> deviceUsers = new ArrayList<>();

    // Audit
    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
