package com.icm.biometric_zone_gate_api.services.impl;

import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.mappers.UserMapper;
import com.icm.biometric_zone_gate_api.models.FingerprintModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.FaceRepository;
import com.icm.biometric_zone_gate_api.repositories.FingerprintRepository;
import com.icm.biometric_zone_gate_api.repositories.PalmRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import com.icm.biometric_zone_gate_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final FingerprintRepository fingerprintRepository;
    private final FaceRepository faceRepository;
    private final PalmRepository palmRepository;

    @Override
    public UserDTO getUserById(Long id) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        return UserMapper.mapToDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        List<UserModel> users = userRepository.findAll();
        return users.stream().map(UserMapper::mapToDTO).toList();
    }

    @Override
    public List<UserModel> syncUsers(List<UserDTO> users) {
        return users.stream()
                .map(this::createUser) // Usa el mismo método createUser
                .toList();
    }

    @Override
    public UserModel createUser(UserDTO dto) {
        UserModel user = new UserModel();
        user.setName(dto.getName());
        user.setPrivilege(dto.getPrivilege());
        user.setCardNumber(dto.getTarjeta());
        user.setPassword(dto.getPwd());
        user.setVaildStart(LocalDate.parse(dto.getVaildStart())); // Cuidado con el formato
        user.setVaildEnd(LocalDate.parse(dto.getVaildEnd()));
        user.setPhotoBase64(dto.getPhoto());

        // Guardar usuario primero
        UserModel savedUser = userRepository.save(user);

        // Guardar huellas
        if (dto.getFps() != null) {
            for (int i = 0; i < dto.getFps().size(); i++) {
                FingerprintModel fp = new FingerprintModel();
                fp.setUserModel(savedUser);
                fp.setIndex(i);
                fp.setBase64(dto.getFps().get(i));
                fingerprintRepository.save(fp);
            }
        }

        // Guardar cara, palma, etc. igual

        return savedUser;
    }

    @Override
    public UserModel updateUser(Long id, UserDTO dto) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        user.setName(dto.getName());
        user.setPrivilege(dto.getPrivilege());
        user.setCardNumber(dto.getTarjeta());
        user.setPassword(dto.getPwd());
        user.setVaildStart(LocalDate.parse(dto.getVaildStart()));
        user.setVaildEnd(LocalDate.parse(dto.getVaildEnd()));
        user.setPhotoBase64(dto.getPhoto());

        // Para simplificar, aquí no actualizamos huellas, rostro ni palma. Puedes extenderlo según tu lógica.

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado con ID: " + id);
        }
        userRepository.deleteById(id);
    }
}

