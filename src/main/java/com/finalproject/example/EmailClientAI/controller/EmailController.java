package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.email.*;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.entity.UserSession;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.security.SecurityUtils;
import com.finalproject.example.EmailClientAI.service.EmailService;
import com.finalproject.example.EmailClientAI.service.GmailService;
import com.finalproject.example.EmailClientAI.service.UserService;
import com.finalproject.example.EmailClientAI.service.impl.PythonEmailClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    private final GmailService gmailService;
    private final UserService userService;
    private final PythonEmailClient pythonEmailClient;
    private final EmailRepository emailRepository;

    @GetMapping("/details/{id}")
    public ResponseEntity<EmailDTO> getEmail(@PathVariable UUID id) {
        var result = emailService.getDetails(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<ListEmailDTO> listEmails(@RequestParam Map<String, String> filters,
                                                             @PageableDefault(
                                                                     size = 10,
                                                                     page = 0,
                                                                     sort = "receivedDate",
                                                                     direction = Sort.Direction.DESC
                                                             ) Pageable pageable) {

        Pageable forced = pageable.getSort().isUnsorted()
                ? PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "receivedDate")
        )
                : pageable;

        var result = emailService.listEmails(filters, forced);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<List<EmailDTO>> getAll(@RequestParam Map<String, String> filters) {

        var result = emailService.listEmails(filters, null);
        var listEmails = result.getEmails();
        return ResponseEntity.ok(listEmails);
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<EmailDTO>> getEmailsByThreadId(@PathVariable String threadId) {
        var result = emailService.getEmailsByThreadId(threadId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{emailId}/attachments/{attachmentId}")
    public ResponseEntity<ByteArrayResource> downloadAttachment(
            @PathVariable String emailId,
            @PathVariable String attachmentId) { // This is the GMAIL attachment ID

        // 1. Get current user & token
        User user = SecurityUtils.getCurrentLoggedInUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserSession session = userService.findActiveSession(user.getId());

        // 2. Call Service
        AttachmentDownloadDTO data = gmailService.downloadAttachment(
                UUID.fromString(emailId),
                UUID.fromString(attachmentId),
                session.getGoogleAccessToken()
        );

        // 3. Construct Response
        ByteArrayResource resource = new ByteArrayResource(data.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(data.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + data.getFilename() + "\"")
                .body(resource);
    }

    @PostMapping("/webhooks/gmail")
    public ResponseEntity<Void> handleGmailWebhook(@RequestBody PubSubMessageDTO pubSubMessageDTO) {
        gmailService.processGmailWebhook(pubSubMessageDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ResponseEntity<Void> sendEmail(@RequestBody GmailSendRequestDTO request) {
        // Authenticate User
        User user = SecurityUtils.getCurrentLoggedInUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        UserSession session = userService.findActiveSession(user.getId());

        // Send
        gmailService.sendEmail(session.getGoogleAccessToken(), request);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{emailId}/status/{statusId}")
    public ResponseEntity<Void> updateEmailStatus(@PathVariable UUID emailId, @PathVariable Long statusId) {
        // Authenticate User
        User user = SecurityUtils.getCurrentLoggedInUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        emailService.updateEmailStatus(user.getId(), emailId, statusId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{emailId}/snooze")
    public ResponseEntity<Void> snoozeEmail(@PathVariable UUID emailId, @RequestParam String until) {
        // Authenticate User
        User user = SecurityUtils.getCurrentLoggedInUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if(until == null || until.isEmpty()) {
            emailService.snoozeEmail(user.getId(), emailId, null );
        }
        else {
            var snoozeUntil = Instant.parse(until);
            emailService.snoozeEmail(user.getId(), emailId, snoozeUntil );
        }


        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{emailId}/unsnooze")
    public ResponseEntity<Void> unSnoozeEmail(@PathVariable UUID emailId) {
        // Authenticate User
        User user = SecurityUtils.getCurrentLoggedInUser()
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        emailService.unSnoozeEmail(user.getId(), emailId );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailDTO>> searchEmails(@RequestParam String q) {
        // 1. Call Python to get IDs and Summaries
        List<PythonEmailClient.SearchResponse> aiResults = pythonEmailClient.search(q);

        if (aiResults.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // 2. Extract IDs
        List<UUID> emailIds = aiResults.stream()
                .map(res -> UUID.fromString(res.getId()))
                .toList();

        List<EmailDTO> fullEmails = emailService.getEmailsByIds(emailIds);

        // 4. Merge Summary & Score, and Sort
        List<EmailDTO> sortedResponse = new ArrayList<>();

        for (PythonEmailClient.SearchResponse aiRes : aiResults) {
            // Find matching full email
            fullEmails.stream()
                    .filter(e -> e.getId().toString().equals(aiRes.getId()))
                    .findFirst()
                    .ifPresent(dto -> {
                        sortedResponse.add(dto);
                    });
        }

        return ResponseEntity.ok(sortedResponse);
    }

    @GetMapping("/summarize")
    public ResponseEntity<String> summarize(@RequestParam String threadId) {
        String rawContent = null;
        if(threadId != null && !threadId.isEmpty()) {
            List<Email> emails = emailRepository.getEmailByThreadId(threadId);
            StringBuilder sb = new StringBuilder();
            for(Email email : emails) {
                sb.append(email.getSubject()).append(email.getSnippet()).append(email.getBodyText()).append("\n");
            }
            rawContent = sb.toString();
        } else {
            throw new AppException(ErrorCode.INVALID_SUMMARY_REQUEST);
        }
        String summary = pythonEmailClient.summarize(rawContent);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<PythonEmailClient.SuggestionResponse>> suggest(@RequestParam String q) {
        return ResponseEntity.ok(pythonEmailClient.suggest(q));
    }

}
