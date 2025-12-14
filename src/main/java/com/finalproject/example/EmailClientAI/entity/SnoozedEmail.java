package com.finalproject.example.EmailClientAI.entity;

import com.finalproject.example.EmailClientAI.enumeration.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "snoozed_emails")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SnoozedEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "email_id", nullable = false, unique = true)
    private UUID emailId;

    @Column(name = "snooze_until", nullable = false)
    private Instant snoozeUntil;

    @Column(name = "previous_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EmailStatus previousStatus;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "email_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Email email;
}
