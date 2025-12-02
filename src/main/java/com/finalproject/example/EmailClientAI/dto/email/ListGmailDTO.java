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
public class ListGmailDTO {
    private List<GmailMessageSummary> messages;
    private String nextPageToken;
    private Long resultSizeEstimate;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GmailMessageSummary {
        private String id;
        private String threadId;
    }
}
