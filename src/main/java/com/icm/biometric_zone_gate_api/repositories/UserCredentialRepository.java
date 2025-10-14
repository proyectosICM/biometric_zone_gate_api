package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.UserCredentialModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredentialModel, Long> {
    Optional<UserCredentialModel> findByDeviceUserIdAndBackupNum(Long deviceUserId, Integer backupNum);
    /*
    List<UserCredentialModel> findByUserId(Long userId);
    Page<UserCredentialModel> findByUserId(Long userId, Pageable pageable);
    */

}
