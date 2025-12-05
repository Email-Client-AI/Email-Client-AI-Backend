package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListEmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.PubSubMessageDTO;
import com.finalproject.example.EmailClientAI.service.EmailService;
import com.finalproject.example.EmailClientAI.service.GmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    private final GmailService gmailService;

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

    @PostMapping("/webhooks/gmail")
    public ResponseEntity<Void> handleGmailWebhook(@RequestBody PubSubMessageDTO pubSubMessageDTO) {
        gmailService.processGmailWebhook(pubSubMessageDTO);
        return ResponseEntity.ok().build();
    }

}
