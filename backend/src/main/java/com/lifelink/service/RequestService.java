package com.lifelink.service;

import com.lifelink.dto.BloodRequestForm;
import com.lifelink.dto.StatusUpdateForm;
import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.RequestStatusHistoryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RequestService {

    private final BloodRequestRepository requests;
    private final RequestStatusHistoryRepository history;
    private final InventoryService inventory;
    private final NotificationService notifications;
    private final AuditService audit;

    public RequestService(BloodRequestRepository requests, RequestStatusHistoryRepository history,
                          InventoryService inventory, NotificationService notifications, AuditService audit) {
        this.requests = requests;
        this.history = history;
        this.inventory = inventory;
        this.notifications = notifications;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- recipient actions
    @Transactional
    public BloodRequest create(BloodRequestForm f, User requester) {
        BloodRequest r = new BloodRequest();
        r.setRequester(requester);
        apply(r, f);
        requests.save(r);
        addHistory(r, RequestStatus.PENDING, "Request created", requester.getEmail());
        String summary = f.getUnits() + " unit(s) of " + f.getBloodGroup().getLabel() + " " + f.getComponent().getLabel()
                + " at " + f.getHospital() + ", " + f.getCity();
        if (f.getUrgency() == Urgency.EMERGENCY) {
            notifications.notifyStaff(NotificationType.EMERGENCY, "EMERGENCY request #" + r.getId() + ": " + summary,
                    "/requests/" + r.getId(), null);
        } else {
            notifications.notifyStaff(NotificationType.REQUEST_STATUS, "New request #" + r.getId() + ": " + summary,
                    "/requests/" + r.getId(), null);
        }
        audit.log(requester.getEmail(), "CREATE_REQUEST", "Request #" + r.getId());
        return r;
    }

    @Transactional
    public BloodRequest update(Long id, BloodRequestForm f, User user) {
        BloodRequest r = getForUser(id, user);
        requireOwner(r, user);
        if (r.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Only requests that are still Pending can be edited");
        }
        apply(r, f);
        r.setUpdatedAt(LocalDateTime.now());
        requests.save(r);
        addHistory(r, r.getStatus(), "Request details updated by requester", user.getEmail());
        audit.log(user.getEmail(), "UPDATE_REQUEST", "Request #" + id);
        return r;
    }

    @Transactional
    public void cancel(Long id, User user) {
        BloodRequest r = getForUser(id, user);
        requireOwner(r, user);
        if (r.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException("Only requests that are still Pending can be cancelled");
        }
        r.setStatus(RequestStatus.CANCELLED);
        r.setUpdatedAt(LocalDateTime.now());
        requests.save(r);
        addHistory(r, RequestStatus.CANCELLED, "Cancelled by requester", user.getEmail());
        audit.log(user.getEmail(), "CANCEL_REQUEST", "Request #" + id);
    }

    // ---------------------------------------------------------------- staff actions
    /**
     * Moves a request along the workflow. Marking it Fulfilled issues the stock in the same database transaction,
     * so a request can never be Fulfilled without the units really leaving the inventory.
     */
    @Transactional
    public BloodRequest changeStatus(Long id, StatusUpdateForm f, User staff) {
        if (staff.getRole() != Role.ADMIN && staff.getRole() != Role.STAFF) {
            throw new AccessDeniedException("Only blood bank staff can change request status");
        }
        BloodRequest r = requests.findById(id).orElseThrow(() -> new NotFoundException("Request not found"));
        RequestStatus next = f.getStatus();
        if (!r.getStatus().canMoveTo(next)) {
            throw new BusinessException("A " + r.getStatus().getLabel() + " request cannot be moved to " + next.getLabel());
        }
        if (next == RequestStatus.REJECTED && !Texts.hasText(f.getRemarks())) {
            throw new BusinessException("Please give a remark explaining why the request is rejected");
        }
        if (next == RequestStatus.FULFILLED) {
            inventory.issue(r.getBloodGroup(), r.getComponent(), r.getUnits(), r.getId(),
                    "Issued for request #" + r.getId(), staff.getEmail());
        }
        r.setStatus(next);
        r.setStaffRemarks(Texts.blankToNull(f.getRemarks()));
        r.setUpdatedAt(LocalDateTime.now());
        requests.save(r);
        addHistory(r, next, f.getRemarks(), staff.getEmail());
        notifications.notify(r.getRequester(), NotificationType.REQUEST_STATUS,
                "Your blood request #" + r.getId() + " is now: " + next.getLabel()
                        + (Texts.hasText(f.getRemarks()) ? " - " + f.getRemarks().trim() : ""),
                "/requests/" + r.getId());
        audit.log(staff.getEmail(), "REQUEST_STATUS", "Request #" + id + " -> " + next);
        return r;
    }

    // ---------------------------------------------------------------- queries
    @Transactional(readOnly = true)
    public BloodRequest getForUser(Long id, User user) {
        BloodRequest r = requests.findById(id).orElseThrow(() -> new NotFoundException("Request not found"));
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        if (!staff && !r.getRequester().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only view your own requests");
        }
        return r;
    }

    @Transactional(readOnly = true)
    public List<RequestStatusHistory> historyOf(Long requestId) {
        return history.findByRequestIdOrderByChangedAtAsc(requestId);
    }

    /** Staff/admin see all requests; everyone else only their own. */
    @Transactional(readOnly = true)
    public List<BloodRequest> search(User user, RequestStatus status, BloodGroup bg, Urgency urgency, String q) {
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        Specification<BloodRequest> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!staff) {
                p.add(cb.equal(root.get("requester").get("id"), user.getId()));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (bg != null) {
                p.add(cb.equal(root.get("bloodGroup"), bg));
            }
            if (urgency != null) {
                p.add(cb.equal(root.get("urgency"), urgency));
            }
            if (Texts.hasText(q)) {
                String like = Texts.like(q);
                p.add(cb.or(cb.like(cb.lower(root.get("patientReference")), like, '\\'),
                        cb.like(cb.lower(root.get("hospital")), like, '\\'),
                        cb.like(cb.lower(root.get("city")), like, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        // Emergency first, then Urgent, then Normal (enum order), newest first within each group.
        List<BloodRequest> result = new ArrayList<>(requests.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")));
        result.sort((a, b) -> Integer.compare(b.getUrgency().ordinal(), a.getUrgency().ordinal()));
        return result;
    }

    // ---------------------------------------------------------------- helpers
    private void requireOwner(BloodRequest r, User user) {
        if (!r.getRequester().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the requester can change this request");
        }
    }

    private void apply(BloodRequest r, BloodRequestForm f) {
        r.setPatientReference(f.getPatientReference().trim());
        r.setBloodGroup(f.getBloodGroup());
        r.setComponent(f.getComponent());
        r.setUnits(f.getUnits());
        r.setHospital(f.getHospital().trim());
        r.setCity(f.getCity().trim());
        r.setContactPhone(f.getContactPhone());
        r.setRequiredDate(f.getRequiredDate());
        r.setUrgency(f.getUrgency());
    }

    private void addHistory(BloodRequest r, RequestStatus status, String remarks, String by) {
        RequestStatusHistory h = new RequestStatusHistory();
        h.setRequest(r);
        h.setStatus(status);
        h.setRemarks(Texts.blankToNull(remarks));
        h.setChangedBy(by);
        history.save(h);
    }
}
