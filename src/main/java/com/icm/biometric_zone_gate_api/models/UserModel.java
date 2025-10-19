package com.icm.biometric_zone_gate_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.icm.biometric_zone_gate_api.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "credentials")
@Entity
@Table(name = "users")
public class UserModel {
    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private String name;

    @Column(unique = true)
    private String email;

    // For web system
    @Column(unique = true)
    private String username;

    // For web system
    private String password;

    // 0=normal, 1=admin, 2=super user
    private Integer adminLevel;

    // true = active, false = disabled
    private Boolean enabled = true;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyModel company;

    //@JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<UserCredentialModel> credentials = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    private ZonedDateTime updatedAt;
}
