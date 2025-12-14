package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListEmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.PubSubMessageDTO;
import com.finalproject.example.EmailClientAI.enumeration.EmailLabel;
import com.finalproject.example.EmailClientAI.enumeration.EmailStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EmailService {
    ListEmailDTO listEmails(Map<String, String>filters, Pageable pageable);

    EmailDTO getDetails(UUID id);

    List<EmailDTO> getEmailsByThreadId(String threadId);

    void updateEmailStatus(UUID userId, UUID emailId, EmailStatus newStatus);

    void snoozeEmail(UUID userId, UUID emailId, Instant snoozeUntil);

    void unSnoozeEmail(UUID userId, UUID emailId);
}
