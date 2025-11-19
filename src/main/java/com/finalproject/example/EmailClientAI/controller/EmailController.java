package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.response.ApiResponse;
import com.finalproject.example.EmailClientAI.dto.response.EmailDetailResponse;
import com.finalproject.example.EmailClientAI.dto.response.EmailListResponse;
import com.finalproject.example.EmailClientAI.dto.response.MailboxResponse;
import com.finalproject.example.EmailClientAI.dto.response.PageResponse;
import com.finalproject.example.EmailClientAI.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailController {

    EmailService emailService;

    @GetMapping("/mailboxes")
    public ApiResponse<List<MailboxResponse>> getMailboxes() {
        return ApiResponse.<List<MailboxResponse>>builder()
                .data(emailService.getMailboxes())
                .build();
    }

    @GetMapping("/mailboxes/{id}/emails")
    public ApiResponse<PageResponse<EmailListResponse>> getEmailsByMailbox(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<EmailListResponse>>builder()
                .data(emailService.getEmailsByMailbox(id, page, size))
                .build();
    }

    @GetMapping("/emails/{id}")
    public ApiResponse<EmailDetailResponse> getEmailDetail(@PathVariable String id) {
        return ApiResponse.<EmailDetailResponse>builder()
                .data(emailService.getEmailDetail(id))
                .build();
    }
}
