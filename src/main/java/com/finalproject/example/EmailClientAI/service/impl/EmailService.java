package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.dto.response.EmailDetailResponse;
import com.finalproject.example.EmailClientAI.dto.response.EmailListResponse;
import com.finalproject.example.EmailClientAI.dto.response.MailboxResponse;
import com.finalproject.example.EmailClientAI.dto.response.PageResponse;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.Mailbox;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.repository.MailboxRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

  EmailRepository emailRepository;
  MailboxRepository mailboxRepository;

  public List<MailboxResponse> getMailboxes() {

    List<Mailbox> mailboxes = mailboxRepository.findAll();

    return mailboxes.stream()
        .map(mailbox -> MailboxResponse.builder()
            .id(mailbox.getId())
            .name(mailbox.getName())
            .unreadCount(mailbox.getUnreadCount())
            .build())
        .collect(Collectors.toList());
  }

  public PageResponse<EmailListResponse> getEmailsByMailbox(Long mailboxId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
    Page<Email> emailPage = emailRepository.findByMailboxId(mailboxId, pageable);

    List<EmailListResponse> emails = emailPage.getContent().stream()
        .map(email -> EmailListResponse.builder()
            .id(email.getId())
            .fromAddress(email.getFromAddress())
            .subject(email.getSubject())
            .preview(email.getPreview())
            .timestamp(email.getTimestamp())
            .isStarred(email.getIsStarred())
            .isRead(email.getIsRead())
            .build())
        .collect(Collectors.toList());

    return PageResponse.<EmailListResponse>builder()
        .content(emails)
        .page(emailPage.getNumber())
        .size(emailPage.getSize())
        .totalElements(emailPage.getTotalElements())
        .totalPages(emailPage.getTotalPages())
        .build();
  }

  public EmailDetailResponse getEmailDetail(String emailId) {
    Email email = emailRepository.findById(emailId)
        .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

    return EmailDetailResponse.builder()
        .id(email.getId())
        .fromAddress(email.getFromAddress())
        .toAddress(email.getToAddress())
        .ccAddress(email.getCcAddress())
        .subject(email.getSubject())
        .body(email.getBody())
        .timestamp(email.getTimestamp())
        .isStarred(email.getIsStarred())
        .isRead(email.getIsRead())
        .build();
  }
}
