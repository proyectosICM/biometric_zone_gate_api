package com.icm.biometric_zone_gate_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLogDTO {
    // "sendlog"
    private String cmd;

    private int count;

    private List<LogRecordDTO> record;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogRecordDTO {
        // ID of the user who made the access attempt
        private int enrollid;

        // Date and time of access - We will convert the JSON string to LocalDateTime
        private LocalDateTime time;

        // Medium used to access: 0=fp, 1=card, 2=password
        private int mode;

        // Access direction: 0=in, 1=out
        private int inout;

        // custom event
        private int event;
    }
}
