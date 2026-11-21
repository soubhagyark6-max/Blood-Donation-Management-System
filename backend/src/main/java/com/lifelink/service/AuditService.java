package com.lifelink.service;

import com.lifelink.model.AuditLog;
import com.lifelink.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void log(String actor, String action, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor == null ? "system" : actor);
        log.setAction(action);
        if (details != null) {
            log.setDetails(details.length() > 500 ? details.substring(0, 500) : details);
        }
        repo.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> recent() {
        return repo.findTop100ByOrderByCreatedAtDesc();
    }
}
