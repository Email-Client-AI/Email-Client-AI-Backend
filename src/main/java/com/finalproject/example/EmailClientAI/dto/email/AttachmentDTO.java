package com.finalproject.example.EmailClientAI.dto.email;


import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentDTO {
    UUID id;
    String fileName;
    String mimeType;
    Long size;
    String gmailAttachmentId;
}
