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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device_user",
        uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "enroll_id"}),
        indexes = {
        @Index(name = "idx_device_user_device_enroll", columnList = "device_id, enroll_id")
})
public class DeviceUserModel {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enroll_id", nullable = false)
    private Integer enrollId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private DeviceModel device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    // 0 = normal, 1 = admin
    private Integer adminLevel = 0;

    private Boolean synced = true;

    @JsonIgnore
    @OneToMany(mappedBy = "deviceUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCredentialModel> credentials = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
