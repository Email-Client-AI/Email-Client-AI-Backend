package com.finalproject.example.EmailClientAI.entity;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "previous_status", referencedColumnName = "id")
    private Status previousStatus;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "email_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Email email;
}
