package com.finalproject.example.EmailClientAI.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailboxResponse {
    Long id;
    String name;
    Integer unreadCount;
}
