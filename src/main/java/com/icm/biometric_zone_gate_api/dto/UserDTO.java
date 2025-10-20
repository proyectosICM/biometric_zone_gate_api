package com.icm.biometric_zone_gate_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String name;
    private String username;
    private String email;
    private String password;
    private Long companyId;
    private Integer adminLevel;
    private Boolean enabled;
    private List<UserCredentialDTO> credentials;
}
