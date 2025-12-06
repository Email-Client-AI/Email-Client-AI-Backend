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
public class AttachmentDownloadDTO {
    private String filename;
    private String mimeType;
    private byte[] data;
}
