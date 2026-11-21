package com.lifelink.repository;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {
    Optional<Donor> findByUserId(Long userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("select d.bloodGroup, count(d) from Donor d group by d.bloodGroup")
    List<Object[]> countGroupedByBloodGroup();
}
