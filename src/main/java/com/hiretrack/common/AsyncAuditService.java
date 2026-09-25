package com.hiretrack.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class AsyncAuditService {

    @Async
    public void logAiInteractionAsync(String userEmail, Long applicationId, String questionSummary) {
        // Asynchronous auditing offloaded to background thread pool
        log.info("[AUDIT-ASYNC] [{}] User: {} | AppId: {} | Question: {}",
                Instant.now(), userEmail, applicationId != null ? applicationId : "GENERAL", questionSummary);
    }
}
