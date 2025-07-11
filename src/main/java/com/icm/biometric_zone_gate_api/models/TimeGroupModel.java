package com.icm.biometric_zone_gate_api.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "time_groups")
public class TimeGroupModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    private UserModel user;

    private String dayOfWeek; // "mon", "tue", "wed", etc.

    @ElementCollection
    private List<String> timeRanges; // ["08301200", "14001800"]
}
