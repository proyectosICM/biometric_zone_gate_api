package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.TimeGroupModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeGroupRepository extends JpaRepository<TimeGroupModel, Long> {
}
