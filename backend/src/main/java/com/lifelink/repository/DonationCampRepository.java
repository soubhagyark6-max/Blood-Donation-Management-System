package com.lifelink.repository;

import com.lifelink.model.DonationCamp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DonationCampRepository extends JpaRepository<DonationCamp, Long> {
    List<DonationCamp> findByActiveTrueAndCampDateGreaterThanEqualOrderByCampDateAscStartTimeAsc(LocalDate from);

    List<DonationCamp> findAllByOrderByCampDateDesc();

    long countByActiveTrueAndCampDateGreaterThanEqual(LocalDate from);
}
