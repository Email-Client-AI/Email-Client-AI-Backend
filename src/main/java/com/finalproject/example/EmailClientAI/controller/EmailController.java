package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.email.*;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.entity.UserSession;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.security.SecurityUtils;
import com.finalproject.example.EmailClientAI.service.AuthenticationService;
import com.finalproject.example.EmailClientAI.service.EmailService;
import com.finalproject.example.EmailClientAI.service.GmailService;
import com.finalproject.example.EmailClientAI.service.UserService;
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

import java.util.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    private final GmailService gmailService;
    private final UserService userService;

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

}
