package com.lifelink.repository;

import com.lifelink.model.BloodRequest;
import com.lifelink.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long>, JpaSpecificationExecutor<BloodRequest> {
    List<BloodRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    long countByStatus(RequestStatus status);

    long countByRequesterId(Long requesterId);

    long countByRequesterIdAndStatus(Long requesterId, RequestStatus status);

    @Query("select r.status, count(r) from BloodRequest r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query("select r.status, count(r) from BloodRequest r where r.requester.id = :uid group by r.status")
    List<Object[]> countGroupedByStatusForUser(Long uid);

    List<BloodRequest> findByCreatedAtGreaterThanEqual(LocalDateTime from);

    List<BloodRequest> findTop8ByOrderByCreatedAtDesc();
}
