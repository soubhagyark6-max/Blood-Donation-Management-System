package com.lifelink.repository;

import com.lifelink.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    List<Donation> findByDonationDateGreaterThanEqual(LocalDate from);

    List<Donation> findTop10ByOrderByCreatedAtDesc();

    long countByDonorId(Long donorId);
}
