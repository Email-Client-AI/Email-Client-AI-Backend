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
public class GmailHistoryResponseDTO {
    private String id;
    private List<GmailMessageSummaryDTO> messages;
}
