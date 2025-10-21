package com.icm.biometric_zone_gate_api.services;

import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import com.icm.biometric_zone_gate_api.repositories.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

    private final UserCredentialRepository userCredentialRepository;

    public List<UserCredentialModel> getAllCredentials() {
        return userCredentialRepository.findAll();
    }

    public Page<UserCredentialModel> getAllCredentials(Pageable pageable) {
        return userCredentialRepository.findAll(pageable);
    }

    public Optional<UserCredentialModel> getCredentialById(Long id) {
        return userCredentialRepository.findById(id);
    }

    public UserCredentialModel createCredential(UserCredentialModel credential) {
        return userCredentialRepository.save(credential);
    }

    public Optional<UserCredentialModel> updateCredential(Long id, UserCredentialModel updatedCredential) {
        return userCredentialRepository.findById(id).map(credential -> {
            credential.setBackupNum(updatedCredential.getBackupNum());
            credential.setRecord(updatedCredential.getRecord());
            credential.setUser(updatedCredential.getUser());
            return userCredentialRepository.save(credential);
        });
    }

    public boolean deleteCredential(Long id) {
        if (userCredentialRepository.existsById(id)) {
            userCredentialRepository.deleteById(id);
            return true;
        }
        return false;
    }


    public List<UserCredentialModel> getCredentialsByUserId(Long userId) {
        return userCredentialRepository.findByUserId(userId);
    }

    public Page<UserCredentialModel> getCredentialsByUserId(Long userId, Pageable pageable) {
        return userCredentialRepository.findByUserId(userId, pageable);
    }

}
