package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.enums.AccessType;
import com.icm.biometric_zone_gate_api.models.AccessLogsModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessLogsRepository extends JpaRepository<AccessLogsModel, Long> {

    @Query("""
                SELECT a FROM AccessLogsModel a
                WHERE a.user.id = :userId
                  AND a.device.id = :deviceId
                  AND a.entryTime = :time
            """)
    Optional<AccessLogsModel> findLogByUserDeviceAndTime(Long userId, Long deviceId, ZonedDateTime time);

    @Query("""
            SELECT a FROM AccessLogsModel a
            WHERE a.user.id = :userId
              AND a.device.id = :deviceId
              AND a.exitTime = :time
            """)
    Optional<AccessLogsModel> findLastClosedLogByUserDevice(Long userId, Long deviceId, ZonedDateTime time);


    List<AccessLogsModel> findByUserId(Long userId);

    Page<AccessLogsModel> findByUserId(Long userId, Pageable pageable);

    List<AccessLogsModel> findByDeviceId(Long deviceId);

    Page<AccessLogsModel> findByDeviceId(Long deviceId, Pageable pageable);

    List<AccessLogsModel> findByCompanyId(Long companyId);

    Page<AccessLogsModel> findByCompanyId(Long companyId, Pageable pageable);

    List<AccessLogsModel> findByAction(AccessType action);

    Page<AccessLogsModel> findByAction(AccessType action, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AccessLogsModel a WHERE a.device.id = :deviceId AND a.entryTime BETWEEN :startOfDay AND :endOfDay")
    long countByDeviceAndDay(
            @Param("deviceId") Long deviceId,
            @Param("startOfDay") ZonedDateTime startOfDay,
            @Param("endOfDay") ZonedDateTime endOfDay
    );

    Page<AccessLogsModel> findByDeviceIdAndEntryTimeBetween(
            Long deviceId,
            ZonedDateTime startOfDay,
            ZonedDateTime endOfDay,
            Pageable pageable
    );

    @Query("""
                SELECT a FROM AccessLogsModel a
                WHERE a.user.id = :userId 
                  AND a.device.id = :deviceId 
                  AND a.exitTime IS NULL
                ORDER BY a.entryTime DESC
                LIMIT 1
            """)
    Optional<AccessLogsModel> findOpenLogByUserAndDevice(@Param("userId") Long userId, @Param("deviceId") Long deviceId);

    List<AccessLogsModel> findByDeviceIdAndCreatedAtBetween(Long deviceId, ZonedDateTime from, ZonedDateTime to);
    List<AccessLogsModel> findByCompanyIdAndCreatedAtBetween(Long companyId, ZonedDateTime from, ZonedDateTime to);
}
