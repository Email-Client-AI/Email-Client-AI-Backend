package com.finalproject.example.EmailClientAI.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailDetailResponse {
    String id;
    String fromAddress;
    String toAddress;
    String ccAddress;
    String subject;
    String body;
    LocalDateTime timestamp;
    Boolean isStarred;
    Boolean isRead;
}
