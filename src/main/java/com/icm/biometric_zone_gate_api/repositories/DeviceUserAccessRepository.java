package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceUserAccessRepository extends JpaRepository<DeviceUserAccessModel, Long> {
    Optional<DeviceUserAccessModel> findByDeviceIdAndEnrollId(Long deviceId, int enrollId);
    List<DeviceUserAccessModel> findByPendingDeleteTrueAndDeviceId(Long deviceId);

    List<DeviceUserAccessModel> findByDeviceIdAndPendingDeleteTrue(Long deviceId);
    Optional<DeviceUserAccessModel> findByEnrollIdAndPendingDeleteTrue(int enrollId);

    Optional<DeviceUserAccessModel> findByenrollId(int userId);

    Optional<DeviceUserAccessModel> findByUserIdAndDeviceId(Long userId, Long deviceId);

    Optional<DeviceUserAccessModel> findByUserIdAndDeviceIdAndEnabledTrue(Long userId, Long deviceId);

    List<DeviceUserAccessModel> findByUserId(Long userId);

    Page<DeviceUserAccessModel> findByUserId(Long userId, Pageable pageable);

    List<DeviceUserAccessModel> findByDeviceId(Long deviceId);

    Page<DeviceUserAccessModel> findByDeviceId(Long deviceId, Pageable pageable);

    List<DeviceUserAccessModel> findByGroupNumber(Integer groupNumber);

    Page<DeviceUserAccessModel> findByGroupNumber(Integer groupNumber, Pageable pageable);

    List<DeviceUserAccessModel> findByEnabledTrue();

    Page<DeviceUserAccessModel> findByEnabledTrue(Pageable pageable);

    Page<DeviceUserAccessModel> findByDeviceIdAndEnabledTrue(Long deviceId, Pageable pageable);

    List<DeviceUserAccessModel> findByDeviceIdAndEnabledTrueAndSyncedFalse(Long deviceId);

    @Query("""
                SELECT a 
                FROM DeviceUserAccessModel a
                JOIN FETCH a.user u
                LEFT JOIN FETCH u.credentials
                WHERE a.device.id = :deviceId
                AND a.enabled = true
                AND a.synced = false
            """)
    List<DeviceUserAccessModel> findPendingWithUserAndCredentials(Long deviceId);

}
