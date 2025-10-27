package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.dto.UserCredentialDTO;
import com.icm.biometric_zone_gate_api.dto.UserDTO;
import com.icm.biometric_zone_gate_api.enums.CredentialType;
import com.icm.biometric_zone_gate_api.mappers.UserMapper;
import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.CompanyRepository;
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
    private final CompanyRepository companyRepository;

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

        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        UserModel savedUser = userRepository.save(user);

        if (savedUser.getCredentials() == null) {
            savedUser.setCredentials(new ArrayList<>(   ));
        }

        // Manejo de credenciales personalizadas
        if (dto.getCredentials() != null && !dto.getCredentials().isEmpty()) {
            for (UserCredentialDTO credDto : dto.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setUser(savedUser);

                String typeString = credDto.getType() != null ? credDto.getType().trim().toUpperCase() : null;
                CredentialType finalType;
                try {
                    finalType = CredentialType.valueOf(typeString);
                } catch (Exception e) {
                    finalType = CredentialType.UNKNOWN;
                }

                credential.setType(finalType);
                credential.setRecord(credDto.getRecord());

                if (credDto.getBackupNum() == null) {
                    switch (finalType) {
                        case PASSWORD -> credential.setBackupNum(10);
                        case CARD -> credential.setBackupNum(11);
                        case FINGERPRINT -> credential.setBackupNum(0);
                        case PHOTO -> credential.setBackupNum(50);
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
    public Optional<UserModel> updateUser(Long id, UserDTO dto) {
        return userRepository.findById(id).map(user -> {

            boolean nameChanged = dto.getName() != null && !dto.getName().equals(user.getName());
            boolean enabledChanged = dto.getEnabled() != null && !dto.getEnabled().equals(user.getEnabled());
            boolean credentialsChanged = false;

            user.setName(dto.getName());
            user.setAdminLevel(dto.getAdminLevel());
            user.setEnabled(dto.getEnabled());
            user.setEnrollId(dto.getEnrollId());

            if (dto.getCompanyId() != null) {
                user.setCompany(companyRepository.findById(dto.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Empresa no encontrada")));
            }

            if (dto.getEmail() != null
                    && !dto.getEmail().isEmpty()
                    && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
                user.setEmail(dto.getEmail());
            }

            if (dto.getUsername() != null
                    && !dto.getUsername().isEmpty()
                    && !dto.getUsername().equalsIgnoreCase(user.getUsername())) {
                user.setUsername(dto.getUsername());
            }

            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            // Manejar credenciales
            if (dto.getCredentials() != null) {
                Map<Long, UserCredentialModel> existingMap = user.getCredentials().stream()
                        .collect(Collectors.toMap(UserCredentialModel::getId, c -> c));

                List<UserCredentialModel> finalList = new ArrayList<>();

                for (UserCredentialDTO cdto : dto.getCredentials()) {
                    UserCredentialModel existing = cdto.getId() != null
                            ? existingMap.get(cdto.getId())
                            : null;

                    if (existing != null) {
                        // Actualizar campos
                        String typeString = cdto.getType() != null ? cdto.getType().trim().toUpperCase() : null;
                        CredentialType finalType;
                        try {
                            finalType = CredentialType.valueOf(typeString);
                        } catch (Exception e) {
                            finalType = CredentialType.UNKNOWN;
                        }
                        existing.setType(finalType);
                        existing.setRecord(cdto.getRecord());
                        existing.setBackupNum(cdto.getBackupNum());
                        finalList.add(existing);
                    } else {
                        // Crear nueva
                        UserCredentialModel newCred = new UserCredentialModel();
                        newCred.setUser(user);
                        String typeString = cdto.getType() != null ? cdto.getType().trim().toUpperCase() : null;
                        CredentialType finalType;
                        try {
                            finalType = CredentialType.valueOf(typeString);
                        } catch (Exception e) {
                            finalType = CredentialType.UNKNOWN;
                        }
                        newCred.setType(finalType);
                        newCred.setRecord(cdto.getRecord());

                        if (cdto.getBackupNum() == null) {
                            switch (newCred.getType()) {
                                case PASSWORD -> newCred.setBackupNum(10);
                                case CARD -> newCred.setBackupNum(11);
                                case PHOTO -> newCred.setBackupNum(50);
                                case FINGERPRINT -> newCred.setBackupNum(0);
                                default -> newCred.setBackupNum(99);
                            }
                        } else {
                            newCred.setBackupNum(cdto.getBackupNum());
                        }

                        finalList.add(newCred);
                    }
                }

                user.getCredentials().clear();
                user.getCredentials().addAll(finalList);
            }

            UserModel saved = userRepository.save(user);

            if (nameChanged) {
                var links = deviceService.getAccessLinksByUserId(saved.getId());
                for (var link : links) {
                    if (Boolean.TRUE.equals(link.isPendingDelete())) continue;
                    link.setPendingNameSync(true);
                    deviceService.saveAccess(link);
                }
            }

            if (enabledChanged) {
                var links = deviceService.getAccessLinksByUserId(saved.getId()); // método helper abajo
                for (var link : links) {
                    // Si está marcado para eliminar, NO programamos enable
                    if (Boolean.TRUE.equals(link.isPendingDelete())) continue;
                    link.setPendingStateSync(true);
                    // opcional: link.setSynced(false); // si quieres reflejar que falta sincronía global
                    deviceService.saveAccess(link);
                }
            }


            if (credentialsChanged) {
                var links = deviceService.getAccessLinksByUserId(saved.getId());
                for (var link : links) {
                    if (Boolean.TRUE.equals(link.isPendingDelete())) continue;
                    link.setSynced(false); // 🟢 esto hace que DeviceSetUserScheduler lo reenvíe
                    deviceService.saveAccess(link);
                }
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

    public Optional<UserModel> findByEnrollId(int enrollId) {
        return userRepository.findByEnrollId(enrollId);
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
