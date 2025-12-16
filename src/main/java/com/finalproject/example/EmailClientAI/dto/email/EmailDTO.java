package com.finalproject.example.EmailClientAI.dto.email;

import com.finalproject.example.EmailClientAI.entity.Status;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailDTO {
    UUID id;
    String gmailEmailId;
    String userId;
    String threadId;
    String snippet;
    String subject;
    String senderEmail;
    Instant receivedDate;
    String aiSummary;

    // You might want to exclude bodyHtml in a "List View" API to save bandwidth,
    // but for a general DTO, it is included.
    String bodyHtml;
    String bodyText;

    Set<String> recipientEmails;
    Set<String> labels;

    List<AttachmentDTO> attachments;
    Status status;
}
