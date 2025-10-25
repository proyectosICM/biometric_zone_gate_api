package com.icm.biometric_zone_gate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceUserAccessDTO {
    private Long id;
    private Integer weekZone;
    private Integer groupNumber;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private Boolean enabled;
    private Long userId;
    private String name;
    private String userName;
    private Long deviceId;
    private String deviceName;
    private int enrollId;

}

