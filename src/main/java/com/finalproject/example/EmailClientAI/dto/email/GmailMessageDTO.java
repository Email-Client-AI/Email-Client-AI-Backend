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
public class GmailMessageDTO {
    private String id;
    private String threadId;
    private List<String> labelIds;
    private String snippet;
    private Long internalDate; // Epoch time in milliseconds
    private MessagePayloadDTO payload;
}
