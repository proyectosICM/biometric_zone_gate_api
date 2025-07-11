package com.icm.biometric_zone_gate_api.mappers;

import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.models.FingerprintModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UserMapper {

    public static UserDTO mapToDTO(UserModel user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getId().toString());
        dto.setName(user.getName());
        dto.setPrivilege(user.getPrivilege());
        dto.setTarjeta(user.getCardNumber());
        dto.setPwd(user.getPassword());
        dto.setPhoto(user.getPhotoBase64());
        dto.setVaildStart(user.getVaildStart().toString());
        dto.setVaildEnd(user.getVaildEnd().toString());

        if (user.getFingerprints() != null) {
            dto.setFps(user.getFingerprints().stream()
                    .map(FingerprintModel::getBase64)
                    .toList());
        }

        return dto;
    }

    public static UserModel toEntity(UserDTO dto) {
        UserModel user = new UserModel();
        user.setName(dto.getName());
        user.setPrivilege(dto.getPrivilege());
        user.setCardNumber(dto.getTarjeta());
        user.setPassword(dto.getPwd());
        user.setVaildStart(LocalDate.parse(dto.getVaildStart()));
        user.setVaildEnd(LocalDate.parse(dto.getVaildEnd()));
        user.setPhotoBase64(dto.getPhoto());
        // huellas y otros pueden ir por separado si quieres mantener limpio el mapper
        return user;
    }
}
