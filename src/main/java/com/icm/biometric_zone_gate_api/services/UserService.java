package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.UserCredentialDTO;
import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.mappers.UserMapper;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final UserCredentialRepository userCredentialRepository;
    private final UserMapper userMapper;

    public List<UserDTO> getAllUsers() {
        List<UserModel> users = userRepository.findAll();

        List<UserDTO> userDTOs = new ArrayList<>();
        for (UserModel user : users) {
            userDTOs.add(userMapper.toDTO(user));
        }

        return userDTOs;
    }

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        Page<UserModel> page = userRepository.findAll(pageable);
        return page.map(userMapper::toDTO);
    }

    public Optional<UserModel> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public UserModel createUser(UserDTO dto) {

        UserModel user = userMapper.toEntity(dto);

        System.out.println("DTO que llega " + dto);
        System.out.println("Usuario que llega " + user);

        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        System.out.println("Antes de crear ");
        UserModel savedUser = userRepository.save(user);
        System.out.println("Usuario creado  ");


        if (savedUser.getCredentials() == null) {
            savedUser.setCredentials(new ArrayList<>(   ));
        }

        // 5️⃣ Manejo de credenciales personalizadas
        if (dto.getCredentials() != null && !dto.getCredentials().isEmpty()) {
            for (UserCredentialDTO credDto : dto.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setUser(savedUser);
                credential.setType(
                        credDto.getType() != null
                                ? CredentialType.valueOf(credDto.getType())
                                : CredentialType.UNKNOWN
                );
                credential.setRecord(credDto.getRecord());

                if (credDto.getBackupNum() == null) {
                    switch (credential.getType()) {
                        case PASSWORD -> credential.setBackupNum(10);
                        case CARD -> credential.setBackupNum(11);
                        case FINGERPRINT -> credential.setBackupNum(0);
                        default -> credential.setBackupNum(99);
                    }
                } else {
                    credential.setBackupNum(credDto.getBackupNum());
                }

                userCredentialRepository.save(credential);
                savedUser.getCredentials().add(credential);
            }
        } else {
            // Credencial por defecto
            UserCredentialModel defaultCredential = new UserCredentialModel();
            defaultCredential.setUser(savedUser);
            defaultCredential.setType(CredentialType.PASSWORD);
            defaultCredential.setBackupNum(10);
            defaultCredential.setRecord("1111");
            userCredentialRepository.save(defaultCredential);
            savedUser.getCredentials().add(defaultCredential);
        }

        return savedUser;
    }
    
    @Transactional
    public Optional<UserModel> updateUser(Long id, UserModel updatedUser) {
        return userRepository.findById(id).map(user -> {
            boolean nameChanged = updatedUser.getName() != null && !updatedUser.getName().equals(user.getName());
            boolean enabledChanged = updatedUser.getEnabled() != null && !updatedUser.getEnabled().equals(user.getEnabled());

            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            user.setAdminLevel(updatedUser.getAdminLevel());
            user.setEnabled(updatedUser.getEnabled());
            user.setCompany(updatedUser.getCompany());

            if (updatedUser.getRole() != null) {
                user.setRole(updatedUser.getRole());
            }

            if (updatedUser.getUsername() != null && !updatedUser.getUsername().isEmpty()) {
                user.setUsername(updatedUser.getUsername());
            }

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            // 🧩 Manejar credenciales actualizadas
            if (updatedUser.getCredentials() != null) {
                Map<Long, UserCredentialModel> existingMap = user.getCredentials().stream()
                        .collect(Collectors.toMap(UserCredentialModel::getId, c -> c));

                List<UserCredentialModel> finalList = new ArrayList<>();

                for (UserCredentialModel newCred : updatedUser.getCredentials()) {
                    UserCredentialModel existing = null;

                    // Si viene con ID, tratamos de actualizar
                    if (newCred.getId() != null) {
                        existing = existingMap.get(newCred.getId());
                    }

                    if (existing != null) {
                        // Actualizar campos
                        existing.setType(newCred.getType() != null ? newCred.getType() : existing.getType());
                        existing.setRecord(newCred.getRecord() != null ? newCred.getRecord() : existing.getRecord());
                        existing.setBackupNum(newCred.getBackupNum() != null ? newCred.getBackupNum() : existing.getBackupNum());
                        finalList.add(existing);
                    } else {
                        // Crear nueva credencial
                        newCred.setUser(user);
                        if (newCred.getType() == null) newCred.setType(CredentialType.UNKNOWN);

                        if (newCred.getBackupNum() == null) {
                            switch (newCred.getType()) {
                                case PASSWORD -> newCred.setBackupNum(10);
                                case CARD -> newCred.setBackupNum(11);
                                case FINGERPRINT -> newCred.setBackupNum(0);
                                default -> newCred.setBackupNum(99);
                            }
                        }
                        finalList.add(newCred);
                    }
                }

                // Reemplaza la lista entera para que JPA elimine las no incluidas
                user.getCredentials().clear();
                user.getCredentials().addAll(finalList);
            }

            UserModel saved = userRepository.save(user);

            // 🔔 Si el nombre cambió, propagar a los dispositivos
            if (nameChanged) {
                deviceService.broadcastUserNameUpdate(saved);
            }

            if (enabledChanged) {
                deviceService.broadcastUserEnableState(saved);
            }

            return saved;


        });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<UserModel> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<UserModel> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<UserModel> getUserByUsernameAndPassword(String username, String password) {
        return userRepository.findByUsernameAndPassword(username, password);
    }

    public List<UserDTO> getUsersByCompanyId(Long companyId) {
        List<UserModel> users = userRepository.findByCompanyId(companyId);

        List<UserDTO> userDTOs = new ArrayList<>();
        for (UserModel user : users) {
            userDTOs.add(userMapper.toDTO(user));
        }

        return userDTOs;
    }

    public Page<UserDTO> getUsersByCompanyId(Long companyId, Pageable pageable) {
        Page<UserModel> page = userRepository.findByCompanyId(companyId, pageable);
        return page.map(userMapper::toDTO);
    }
}
