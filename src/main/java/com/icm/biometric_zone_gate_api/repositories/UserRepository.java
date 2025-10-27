package com.icm.biometric_zone_gate_api.repositories;

import com.icm.biometric_zone_gate_api.models.CompanyModel;
import com.icm.biometric_zone_gate_api.models.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByNameAndCompany(String name, CompanyModel companyModel);

    Optional<UserModel> findByEnrollIdAndCompany(int enrollId, CompanyModel company);

    Optional<UserModel> findByEnrollId(int enrollId);

    Optional<UserModel> findByName(String name);

    Optional<UserModel> findByUsername(String username);

    Optional<UserModel> findByEmail(String email);

    Optional<UserModel> findByUsernameAndPassword(String username, String password);

    List<UserModel> findByCompanyId(Long id);

    Page<UserModel> findByCompanyId(Long id, Pageable pageable);

}
