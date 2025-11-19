package com.finalproject.example.EmailClientAI.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "emails")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "mailbox_id", nullable = false)
    Long mailboxId;

    @Column(name = "from_address", nullable = false)
    String fromAddress;

    @Column(name = "to_address", nullable = false)
    String toAddress;

    @Column(name = "cc_address")
    String ccAddress;

    @Column(nullable = false)
    String subject;

    @Column(length = 500)
    String preview;

    @Column(columnDefinition = "TEXT", nullable = false)
    String body;

    @Column(nullable = false)
    LocalDateTime timestamp;

    @Column(name = "is_starred")
    @Builder.Default
    Boolean isStarred = false;

    @Column(name = "is_read")
    @Builder.Default
    Boolean isRead = false;

    @Column(name = "user_id", nullable = false)
    String userId;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
