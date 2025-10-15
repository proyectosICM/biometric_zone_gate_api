package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.UserModel;
import com.icm.biometric_zone_gate_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeviceService deviceService;

    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<UserModel> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<UserModel> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public UserModel createUser(UserModel user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public Optional<UserModel> updateUser(Long id, UserModel updatedUser) {
        return userRepository.findById(id).map(user -> {
            boolean nameChanged = updatedUser.getName() != null && !updatedUser.getName().equals(user.getName());
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

            UserModel saved = userRepository.save(user);

            // 🔔 Si el nombre cambió, propagar a los dispositivos
            if (nameChanged) {
                deviceService.broadcastUserNameUpdate(saved);
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
