package com.lifelink.repository;

import com.lifelink.model.BloodGroup;
import com.lifelink.model.BloodInventory;
import com.lifelink.model.ComponentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long>, JpaSpecificationExecutor<BloodInventory> {

    /** Usable (not expired) stock per group + component. */
    @Query("select i.bloodGroup, i.component, sum(i.units) from BloodInventory i " +
            "where i.units > 0 and i.expiryDate >= :today group by i.bloodGroup, i.component")
    List<Object[]> usableStockSummary(@Param("today") LocalDate today);

    @Query("select coalesce(sum(i.units), 0) from BloodInventory i where i.units > 0 and i.expiryDate >= :today " +
            "and i.bloodGroup = :bg and i.component = :comp")
    long usableUnits(@Param("bg") BloodGroup bg, @Param("comp") ComponentType comp, @Param("today") LocalDate today);

    /** Locks the usable batches (earliest expiry first) so concurrent issues cannot overdraw stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BloodInventory i where i.bloodGroup = :bg and i.component = :comp " +
            "and i.units > 0 and i.expiryDate >= :today order by i.expiryDate asc, i.id asc")
    List<BloodInventory> lockUsableBatches(@Param("bg") BloodGroup bg, @Param("comp") ComponentType comp,
                                           @Param("today") LocalDate today);

    /** Batches that still hold stock and expire between today and the given date (inclusive). */
    @Query("select i from BloodInventory i where i.units > 0 and i.expiryDate >= :today and i.expiryDate <= :until " +
            "order by i.expiryDate asc")
    List<BloodInventory> expiringBetween(@Param("today") LocalDate today, @Param("until") LocalDate until);

    /** Batches already expired that still hold stock (should be discarded). */
    List<BloodInventory> findByUnitsGreaterThanAndExpiryDateBefore(int units, LocalDate date);

    @Query("select coalesce(sum(i.units), 0) from BloodInventory i where i.units > 0 and i.expiryDate >= :today")
    long totalUsableUnits(@Param("today") LocalDate today);
}
