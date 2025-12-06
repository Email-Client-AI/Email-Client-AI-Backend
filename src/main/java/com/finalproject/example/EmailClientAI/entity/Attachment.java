package com.finalproject.example.EmailClientAI.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "attachments")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "email_id", nullable = false)
    private UUID emailId;

    @Column(name = "filename", length = 2000)
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size")
    private Long size;

    @Column(name = "gmail_attachment_id", columnDefinition = "TEXT")
    private String gmailAttachmentId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "email_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Email email;
}
