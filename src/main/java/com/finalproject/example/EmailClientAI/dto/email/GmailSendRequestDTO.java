package com.finalproject.example.EmailClientAI.dto.email;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GmailSendRequestDTO {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String bodyHtml;

    // Optional: Only used when replying
    private String replyToEmailId;
}
