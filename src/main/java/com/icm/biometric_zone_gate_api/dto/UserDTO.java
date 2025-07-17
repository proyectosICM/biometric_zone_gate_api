package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

// Registro y Gestion de usuarios usuarios
@Data
public class UserDTO {
    private String userId;
    private String name;
    private Integer privilege;
    private String tarjeta;
    private String pwd;
    private List<String> fps;      // Huellas (Base64)
    private String face;           // Base64
    private String palm;          // Base64
    private String photo;           // Base64
    private String vaildStart;     // "aaaaMMdd"
    private String vaildEnd;       // "aaaaMMdd"
    private Map<String, List<String>> timeGroups;
    private Integer update;
    private Integer photoEnroll;
}
