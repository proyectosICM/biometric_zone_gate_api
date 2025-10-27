    package com.icm.biometric_zone_gate_api.models;
    
    import com.icm.biometric_zone_gate_api.enums.AccessType;
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
    @Table(name = "zone_access_logs")
    public class AccessLogsModel {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private ZonedDateTime entryTime;
    
        @Column
        private ZonedDateTime exitTime;
    
        @Column
        private Long durationSeconds;
    
        //(fetch = FetchType.LAZY)
        @ManyToOne
        @JoinColumn(name = "event_type_id", nullable = false)
        private EventTypeModel eventType;

        private Boolean correctEpp;
    
        @Column(nullable = false)
        private Boolean success;
    
        private String observation;
    
        @ManyToOne
        @JoinColumn(name = "user_id", nullable = false)
        private UserModel user;
    
        @ManyToOne
        @JoinColumn(name = "device_id", nullable = false)
        private DeviceModel device;
    
        @ManyToOne
        @JoinColumn(name = "company_id", nullable = false)
        private CompanyModel company;
    
        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AccessType action;
    
        @Column(nullable = false, updatable = false)
        @CreationTimestamp
        private ZonedDateTime createdAt;
    
        @UpdateTimestamp
        private ZonedDateTime updatedAt;
    }
