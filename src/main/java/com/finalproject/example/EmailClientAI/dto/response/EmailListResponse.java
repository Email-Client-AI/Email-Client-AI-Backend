package com.finalproject.example.EmailClientAI.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailListResponse {
    String id;
    String fromAddress;
    String subject;
    String preview;
    LocalDateTime timestamp;
    Boolean isStarred;
    Boolean isRead;
}
