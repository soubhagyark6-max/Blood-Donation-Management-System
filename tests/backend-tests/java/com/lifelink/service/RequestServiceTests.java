package com.lifelink.service;

import com.lifelink.TestSupport;
import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.StatusUpdateForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RequestServiceTests {

    @Autowired RequestService requests;
    @Autowired InventoryService inventory;
    @Autowired TestSupport support;
    @Autowired com.lifelink.repository.NotificationRepository notifications;

    User recipient;
    User staff;

    @BeforeEach
    void setUp() {
        recipient = support.user(Role.RECIPIENT);
        staff = support.user(Role.STAFF);
    }

    private StatusUpdateForm status(RequestStatus s, String remarks) {
        StatusUpdateForm f = new StatusUpdateForm();
        f.setStatus(s);
        f.setRemarks(remarks);
        return f;
    }

    @Test
    void createStartsPendingWithHistory() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 2, Urgency.NORMAL), recipient);
        assertEquals(RequestStatus.PENDING, r.getStatus());
        assertEquals(1, requests.historyOf(r.getId()).size());
    }

    @Test
    void emergencyRequestNotifiesStaff() {
        long before = notifications.countByUserIdAndReadFalse(staff.getId());
        requests.create(TestSupport.requestForm(BloodGroup.O_NEG, ComponentType.WHOLE_BLOOD, 1, Urgency.EMERGENCY), recipient);
        assertEquals(before + 1, notifications.countByUserIdAndReadFalse(staff.getId()));
    }

    @Test
    void workflowAllowsOnlyValidTransitions() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        // Pending -> Fulfilled is not allowed
        assertThrows(BusinessException.class, () -> requests.changeStatus(r.getId(), status(RequestStatus.FULFILLED, null), staff));
        requests.changeStatus(r.getId(), status(RequestStatus.UNDER_REVIEW, "checking"), staff);
        requests.changeStatus(r.getId(), status(RequestStatus.APPROVED, null), staff);
        assertEquals(RequestStatus.APPROVED, r.getStatus());
        assertEquals(3, requests.historyOf(r.getId()).size());
    }

    @Test
    void rejectionNeedsRemark() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.B_POS, ComponentType.PLASMA, 1, Urgency.NORMAL), recipient);
        assertThrows(BusinessException.class, () -> requests.changeStatus(r.getId(), status(RequestStatus.REJECTED, "  "), staff));
        requests.changeStatus(r.getId(), status(RequestStatus.REJECTED, "Documents missing"), staff);
        assertEquals(RequestStatus.REJECTED, r.getStatus());
        assertEquals("Documents missing", r.getStaffRemarks());
    }

    @Test
    void fulfilIssuesStockInSameTransaction() {
        inventory.receive(BloodGroup.O_POS, ComponentType.RED_CELLS, 10, LocalDate.now(), null, null, "tester", false);
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.O_POS, ComponentType.RED_CELLS, 3, Urgency.URGENT), recipient);
        requests.changeStatus(r.getId(), status(RequestStatus.APPROVED, null), staff);
        requests.changeStatus(r.getId(), status(RequestStatus.FULFILLED, "issued"), staff);
        assertEquals(RequestStatus.FULFILLED, r.getStatus());
        assertEquals(7, inventory.stockMatrix().get(BloodGroup.O_POS).get(ComponentType.RED_CELLS));
    }

    @Test
    void fulfilFailsWhenStockInsufficientAndStatusStaysApproved() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.AB_NEG, ComponentType.PLATELETS, 2, Urgency.NORMAL), recipient);
        requests.changeStatus(r.getId(), status(RequestStatus.APPROVED, null), staff);
        assertThrows(BusinessException.class, () -> requests.changeStatus(r.getId(), status(RequestStatus.FULFILLED, null), staff));
        assertEquals(RequestStatus.APPROVED, r.getStatus());
    }

    @Test
    void recipientCannotChangeStatus() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        assertThrows(AccessDeniedException.class, () -> requests.changeStatus(r.getId(), status(RequestStatus.APPROVED, null), recipient));
    }

    @Test
    void otherRecipientCannotViewOrCancel() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        User other = support.user(Role.RECIPIENT);
        assertThrows(AccessDeniedException.class, () -> requests.getForUser(r.getId(), other));
        assertThrows(AccessDeniedException.class, () -> requests.cancel(r.getId(), other));
        assertNotNull(requests.getForUser(r.getId(), staff));
    }

    @Test
    void ownerCanUpdateAndCancelWhilePendingOnly() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        BloodRequestForm f = TestSupport.requestForm(BloodGroup.B_POS, ComponentType.PLASMA, 4, Urgency.URGENT);
        requests.update(r.getId(), f, recipient);
        assertEquals(BloodGroup.B_POS, r.getBloodGroup());
        assertEquals(4, r.getUnits());

        requests.changeStatus(r.getId(), status(RequestStatus.UNDER_REVIEW, null), staff);
        assertThrows(BusinessException.class, () -> requests.update(r.getId(), f, recipient));
        assertThrows(BusinessException.class, () -> requests.cancel(r.getId(), recipient));
    }

    @Test
    void pendingRequestCanBeCancelled() {
        BloodRequest r = requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        requests.cancel(r.getId(), recipient);
        assertEquals(RequestStatus.CANCELLED, r.getStatus());
    }

    @Test
    void recipientSearchOnlySeesOwnRequests() {
        User other = support.user(Role.RECIPIENT);
        requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        requests.create(TestSupport.requestForm(BloodGroup.B_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), other);
        assertEquals(1, requests.search(recipient, null, null, null, null).size());
        assertEquals(2, requests.search(staff, null, null, null, null).size());
    }

    @Test
    void emergencyRequestsAreListedFirst() {
        requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.NORMAL), recipient);
        requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.EMERGENCY), recipient);
        requests.create(TestSupport.requestForm(BloodGroup.A_POS, ComponentType.WHOLE_BLOOD, 1, Urgency.URGENT), recipient);
        var list = requests.search(staff, null, null, null, null);
        assertEquals(Urgency.EMERGENCY, list.get(0).getUrgency());
        assertEquals(Urgency.URGENT, list.get(1).getUrgency());
        assertEquals(Urgency.NORMAL, list.get(2).getUrgency());
    }
}
