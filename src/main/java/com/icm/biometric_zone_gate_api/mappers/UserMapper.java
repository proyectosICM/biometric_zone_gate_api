package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.UserCredentialDTO;
import com.icm.biometric_zone_gate_api.dto.UserDTO;
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
        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setAdminLevel(dto.getAdminLevel());
        user.setEnabled(dto.getEnabled());
        user.setEnrollId(dto.getEnrollId());


        if (dto.getCompanyId() != null) {
            user.setCompany(companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada")));
        }

        user.setCredentials(new ArrayList<>());
        return user;
    }

    public UserDTO toDTO(UserModel user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setAdminLevel(user.getAdminLevel());
        dto.setEnabled(user.getEnabled());
        dto.setEnrollId(user.getEnrollId());

        if (user.getCompany() != null) {
            dto.setCompanyId(user.getCompany().getId());
            dto.setCompanyName(user.getCompany().getName());
        }


        if (user.getCredentials() != null && !user.getCredentials().isEmpty()) {
            List<UserCredentialDTO> creds = new ArrayList<>();
            for (UserCredentialModel c : user.getCredentials()) {
                UserCredentialDTO cdto = new UserCredentialDTO();
                cdto.setId(c.getId());
                cdto.setType(c.getType() != null ? c.getType().toString() : "UNKNOWN");
                cdto.setBackupNum(c.getBackupNum());
                cdto.setRecord(c.getRecord());
                creds.add(cdto);
            }
            dto.setCredentials(creds);
        } else {
            dto.setCredentials(new ArrayList<>());
        }

        return dto;
    }

}
