package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.UserCredentialDTO;
import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final CompanyRepository companyRepository;

    public UserModel toEntity(UserDTO dto) {
        UserModel user = new UserModel();

        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setAdminLevel(dto.getAdminLevel());
        user.setEnabled(dto.getEnabled());

        if (dto.getCompanyId() != null) {
            user.setCompany(companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada")));
        }

        user.setCredentials(new ArrayList<>());

        return user;
    }
}
