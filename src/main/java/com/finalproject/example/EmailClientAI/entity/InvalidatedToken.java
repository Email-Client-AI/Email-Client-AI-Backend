package com.finalproject.example.EmailClientAI.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "invalidated_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvalidatedToken {
    @Id
    @Column(name = "access_id")
    String accessId;

    @Column(name = "refresh_id", nullable = false)
    String refreshId;

    @Column(name = "expiration_time", nullable = false)
    LocalDateTime expirationTime;
}
