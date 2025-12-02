package com.finalproject.example.EmailClientAI.dto.email;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageBodyDTO {
    private Integer size;
    private String data;         // Base64 Encoded content
    private String attachmentId; // Present if it's an attachment
}
