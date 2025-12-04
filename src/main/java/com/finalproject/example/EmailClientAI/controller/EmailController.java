package com.finalproject.example.EmailClientAI.controller;

import com.finalproject.example.EmailClientAI.dto.email.EmailDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListEmailDTO;
import com.finalproject.example.EmailClientAI.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;
    @GetMapping("/details/{id}")
    public ResponseEntity<EmailDTO> getEmail(@PathVariable UUID id) {


        var result = emailService.getDetails(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<ListEmailDTO> listEmails(@RequestParam Map<String, String> filters,
                                                             @PageableDefault(size = 10, page = 0, direction = Sort.Direction.ASC) Pageable pageable) {

        var result = emailService.listEmails(filters, pageable);
        return ResponseEntity.ok(result);
    }
}
