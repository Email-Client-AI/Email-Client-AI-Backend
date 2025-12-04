package com.finalproject.example.EmailClientAI.entity;

import com.finalproject.example.EmailClientAI.enumeration.EmailLabel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "emails")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "gmail_email_id", nullable = false, unique = true)
    String gmailEmailId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "thread_id")
    String threadId;

    @Column(length = 1000)
    String snippet;

    @Column(name = "subject", length = 2000)
    String subject;

    @Column(name = "sender_email")
    String senderEmail;

    @Column(name = "received_date")
    Instant receivedDate;

    @Column(name = "body_html", columnDefinition = "TEXT")
    String bodyHtml;

    @Column(name = "body_text", columnDefinition = "TEXT")
    String bodyText;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "email_recipients", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "recipient_email")
    Set<String> recipientEmails = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "email_labels", joinColumns = @JoinColumn(name = "email_id"))
    @Column(name = "label")
    Set<String> labels = new HashSet<>();

    // Attachments
    @OneToMany(mappedBy = "email", fetch = FetchType.EAGER,cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Attachment> attachments = new ArrayList<>();
}
