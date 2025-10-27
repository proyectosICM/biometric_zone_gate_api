package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.DeviceUserAccessModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviceUserAccessRepository extends JpaRepository<DeviceUserAccessModel, Long> {

    @Modifying
    @Query("DELETE FROM DeviceUserAccessModel a WHERE a.device.id = :deviceId")
    void deleteByDeviceId(Long deviceId);

    Optional<DeviceUserAccessModel> findByDeviceIdAndEnrollId(Long deviceId, int enrollId);

    List<DeviceUserAccessModel> findByPendingDeleteTrueAndDeviceId(Long deviceId);

    Optional<DeviceUserAccessModel> findByDeviceSnAndEnrollIdAndPendingDeleteFalse(String sn, int enrollId);

    @Query("""
       select dua
       from DeviceUserAccessModel dua
       join dua.device d
       where d.sn = :sn
         and dua.enrollId = :enrollId
         and dua.pendingDelete = true
       """)
    Optional<DeviceUserAccessModel> findByDeviceSnAndEnrollIdAndPendingDeleteTrue(String sn, int enrollId);

    @Query("""
       select dua
       from DeviceUserAccessModel dua
       join fetch dua.user u
       where dua.device.id = :deviceId
         and dua.pendingNameSync = true
       """)
    List<DeviceUserAccessModel> findPendingNameSyncWithUser(Long deviceId);


    @Query("""
           select dua
           from DeviceUserAccessModel dua
           join fetch dua.user u
           join dua.device d
           where d.sn = :sn
             and dua.enrollId = :enrollId
             and dua.pendingDelete = false
           """)
    Optional<DeviceUserAccessModel> findByDeviceSnAndEnrollIdAndPendingDeleteFalseFetchUser(String sn, int enrollId);


    List<DeviceUserAccessModel> findByDeviceIdAndPendingDeleteTrue(Long deviceId);

    Optional<DeviceUserAccessModel> findByEnrollIdAndPendingDeleteTrue(int enrollId);

    Optional<DeviceUserAccessModel> findByEnrollIdAndPendingDeleteFalse(int enrollId);

    Optional<DeviceUserAccessModel> findByenrollId(int userId);

    Optional<DeviceUserAccessModel> findByUserIdAndDeviceId(Long userId, Long deviceId);

    Optional<DeviceUserAccessModel> findByUserIdAndDeviceIdAndEnabledTrue(Long userId, Long deviceId);

    List<DeviceUserAccessModel> findByUserId(Long userId);

    Page<DeviceUserAccessModel> findByUserId(Long userId, Pageable pageable);

    List<DeviceUserAccessModel> findByDeviceId(Long deviceId);

    Page<DeviceUserAccessModel> findByDeviceId(Long deviceId, Pageable pageable);

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

    @Query("""
            SELECT DISTINCT a FROM DeviceUserAccessModel a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.credentials c
            WHERE a.device.id = :deviceId
            AND a.enabled = true
            AND a.synced = false
            AND a.pendingDelete = false
            """)
    List<DeviceUserAccessModel> findPendingWithUserAndCredentialsAndPendingDeleteFalse(Long deviceId);

    @Query("""
                SELECT a FROM DeviceUserAccessModel a
                JOIN FETCH a.user u
                JOIN FETCH u.credentials c
                WHERE a.device.id = :deviceId
                  AND a.pendingDelete = true
                  AND a.synced = true
            """)
    List<DeviceUserAccessModel> findPendingDeleteWithUserAndCredentials(Long deviceId);

    // 🔹 Para scheduler ENABLE: trae accesos de este device con pendingStateSync = true y NO pendingDelete
    @Query("""
           select dua
           from DeviceUserAccessModel dua
           join fetch dua.user u
           where dua.device.id = :deviceId
             and dua.pendingStateSync = true
             and dua.pendingDelete = false
           """)
    List<DeviceUserAccessModel> findPendingStateSyncWithUser(Long deviceId);


}
