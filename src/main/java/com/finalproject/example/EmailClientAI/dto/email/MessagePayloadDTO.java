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
public class MessagePayloadDTO {
    private String mimeType;
    private String filename; // Only present if it's an attachment
    private List<MessageHeaderDTO> headers;
    private MessageBodyDTO body;
    private List<MessagePayloadDTO> parts; // The recursive tree!
}
