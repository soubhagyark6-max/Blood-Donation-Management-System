package com.lifelink.service;

import com.lifelink.TestSupport;
import com.lifelink.dto.StockForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import com.lifelink.repository.BloodInventoryRepository;
import com.lifelink.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InventoryServiceTests {

    @Autowired InventoryService inventory;
    @Autowired BloodInventoryRepository batches;
    @Autowired InventoryTransactionRepository txRepo;

    private long stock(BloodGroup g, ComponentType c) {
        return inventory.stockMatrix().get(g).get(c);
    }

    @Test
    void receiveAddsUsableStockAndLogsTransaction() {
        long before = stock(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD);
        long txBefore = txRepo.count();
        inventory.receive(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 6, LocalDate.now(), null, "test", "tester", false);
        assertEquals(before + 6, stock(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD));
        assertEquals(txBefore + 1, txRepo.count());
    }

    @Test
    void issueDeductsStock() {
        inventory.receive(BloodGroup.B_POS, ComponentType.RED_CELLS, 10, LocalDate.now(), null, null, "tester", false);
        inventory.issue(BloodGroup.B_POS, ComponentType.RED_CELLS, 4, null, "test", "tester");
        assertEquals(6, stock(BloodGroup.B_POS, ComponentType.RED_CELLS));
    }

    @Test
    void issueMoreThanAvailableIsRejectedAndStockUnchanged() {
        inventory.receive(BloodGroup.O_NEG, ComponentType.PLASMA, 3, LocalDate.now(), null, null, "tester", false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> inventory.issue(BloodGroup.O_NEG, ComponentType.PLASMA, 5, null, "test", "tester"));
        assertTrue(ex.getMessage().contains("Not enough usable stock"));
        assertEquals(3, stock(BloodGroup.O_NEG, ComponentType.PLASMA));
    }

    @Test
    void componentsAreNotInterchangeable() {
        inventory.receive(BloodGroup.AB_POS, ComponentType.WHOLE_BLOOD, 20, LocalDate.now(), null, null, "tester", false);
        // plenty of whole blood must not satisfy a platelet issue
        assertThrows(BusinessException.class,
                () -> inventory.issue(BloodGroup.AB_POS, ComponentType.PLATELETS, 1, null, null, "tester"));
        assertEquals(20, stock(BloodGroup.AB_POS, ComponentType.WHOLE_BLOOD));
    }

    @Test
    void issueUsesEarliestExpiryFirstAcrossBatches() {
        LocalDate today = LocalDate.now();
        BloodInventory late = inventory.receive(BloodGroup.A_NEG, ComponentType.RED_CELLS, 5, today.minusDays(1), today.plusDays(30), null, "tester", false);
        BloodInventory early = inventory.receive(BloodGroup.A_NEG, ComponentType.RED_CELLS, 5, today.minusDays(2), today.plusDays(10), null, "tester", false);
        inventory.issue(BloodGroup.A_NEG, ComponentType.RED_CELLS, 7, null, null, "tester");
        assertEquals(0, batches.findById(early.getId()).orElseThrow().getUnits());
        assertEquals(3, batches.findById(late.getId()).orElseThrow().getUnits());
    }

    @Test
    void expiredBatchesAreNotUsable() {
        BloodInventory expired = new BloodInventory();
        expired.setBatchCode("TEST-EXPIRED");
        expired.setBloodGroup(BloodGroup.B_NEG);
        expired.setComponent(ComponentType.PLATELETS);
        expired.setUnits(9);
        expired.setUnitsReceived(9);
        expired.setCollectionDate(LocalDate.now().minusDays(20));
        expired.setExpiryDate(LocalDate.now().minusDays(1));
        batches.save(expired);

        assertEquals(0, stock(BloodGroup.B_NEG, ComponentType.PLATELETS));
        assertThrows(BusinessException.class,
                () -> inventory.issue(BloodGroup.B_NEG, ComponentType.PLATELETS, 1, null, null, "tester"));

        int discarded = inventory.discardExpired("tester");
        assertEquals(9, discarded);
        assertEquals(0, batches.findById(expired.getId()).orElseThrow().getUnits());
    }

    @Test
    void validationRejectsBadReceiveInput() {
        assertThrows(BusinessException.class, () -> inventory.receive(BloodGroup.A_POS, ComponentType.PLASMA, 0, null, null, null, "t", false));
        assertThrows(BusinessException.class, () -> inventory.receive(BloodGroup.A_POS, ComponentType.PLASMA, 1,
                LocalDate.now().plusDays(2), null, null, "t", false)); // future collection date
        assertThrows(BusinessException.class, () -> inventory.receive(BloodGroup.A_POS, ComponentType.PLASMA, 1,
                LocalDate.now(), LocalDate.now().minusDays(1), null, "t", false)); // expiry before collection
    }

    @Test
    void lowStockIsReportedBelowThreshold() {
        inventory.receive(BloodGroup.AB_NEG, ComponentType.PLATELETS, 2, LocalDate.now(), null, null, "tester", false);
        assertTrue(inventory.lowStock().stream().anyMatch(l ->
                l.group() == BloodGroup.AB_NEG && l.component() == ComponentType.PLATELETS && l.units() == 2));
    }

    @Test
    void expiringSoonListsNearExpiryBatches() {
        LocalDate today = LocalDate.now();
        inventory.receive(BloodGroup.O_POS, ComponentType.RED_CELLS, 3, today.minusDays(30), today.plusDays(2), null, "tester", false);
        assertFalse(inventory.expiringSoon().isEmpty());
    }

    @Test
    void stockFormIssueWorks() {
        inventory.receive(BloodGroup.A_POS, ComponentType.PLASMA, 8, LocalDate.now(), null, null, "tester", false);
        StockForm f = new StockForm();
        f.setBloodGroup(BloodGroup.A_POS);
        f.setComponent(ComponentType.PLASMA);
        f.setUnits(2);
        inventory.issue(f, "tester");
        assertEquals(6, stock(BloodGroup.A_POS, ComponentType.PLASMA));
    }
}
