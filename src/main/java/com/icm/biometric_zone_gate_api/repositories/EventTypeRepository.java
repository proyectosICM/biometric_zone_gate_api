package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.EventTypeModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventTypeRepository extends JpaRepository<EventTypeModel, Long> {
    boolean existsByCode(int code);
    Optional<EventTypeModel> findByCode(int code);
    EventTypeModel findFirstByOrderByIdAsc();
}
