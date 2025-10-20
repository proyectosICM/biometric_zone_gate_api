package com.icm.biometric_zone_gate_api.services;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;
    private final UserCredentialRepository userCredentialRepository;
    private final UserMapper userMapper;

    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<UserModel> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
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

        System.out.println("DTO que llega " + dto);
        System.out.println("Usuario que llega " + user);


        // 5️⃣ Manejo de credenciales personalizadas
        if (dto.getCredentials() != null && !dto.getCredentials().isEmpty()) {
            for (UserCredentialModel credential : dto.getCredentials()) {
                credential.setUser(savedUser);
                if (credential.getType() == null) {
                    credential.setType(CredentialType.UNKNOWN);
                }

                // Asignar backupNum según tipo si no se envió
                if (credential.getBackupNum() == null) {
                    switch (credential.getType()) {
                        case PASSWORD -> credential.setBackupNum(10);
                        case CARD -> credential.setBackupNum(11);
                        case FINGERPRINT -> credential.setBackupNum(0);
                        default -> credential.setBackupNum(99);
                    }
                }

                // Guardamos la credencial
                userCredentialRepository.save(credential);
                savedUser.getCredentials().add(credential);
            }
        } else {
            // 6️⃣ Si no hay credenciales, creamos una por defecto
            UserCredentialModel defaultCredential = new UserCredentialModel();
            defaultCredential.setUser(savedUser);
            defaultCredential.setType(CredentialType.PASSWORD);
            defaultCredential.setBackupNum(10);
            defaultCredential.setRecord("1111");

            userCredentialRepository.save(defaultCredential);
            savedUser.getCredentials().add(defaultCredential);
        }

        // 7️⃣ Retornamos el usuario con sus credenciales correctamente enlazadas
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
            if (updatedUser.getCredentials() != null && !updatedUser.getCredentials().isEmpty()) {
                // Eliminar credenciales anteriores (para simplificar sincronización)
                userCredentialRepository.deleteAll(user.getCredentials());
                user.getCredentials().clear();

                for (UserCredentialModel newCred : updatedUser.getCredentials()) {
                    newCred.setUser(user);
                    if (newCred.getType() == null) newCred.setType(CredentialType.UNKNOWN);

                    if (newCred.getBackupNum() == null) {
                        if (newCred.getType() == CredentialType.PASSWORD) newCred.setBackupNum(10);
                        else if (newCred.getType() == CredentialType.CARD) newCred.setBackupNum(11);
                        else newCred.setBackupNum(0);
                    }

                    userCredentialRepository.save(newCred);
                    user.getCredentials().add(newCred);
                }
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

    public List<UserModel> getUsersByCompanyId(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    public Page<UserModel> getUsersByCompanyId(Long companyId, Pageable pageable) {
        return userRepository.findByCompanyId(companyId, pageable);
    }
}
