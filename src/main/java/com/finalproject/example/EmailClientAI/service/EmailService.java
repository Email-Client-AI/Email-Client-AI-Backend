package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListEmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.PubSubMessageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EmailService {
    ListEmailDTO listEmails(Map<String, String>filters, Pageable pageable);

    EmailDTO getDetails(UUID id);

    List<EmailDTO> getEmailsByThreadId(String threadId);
}
