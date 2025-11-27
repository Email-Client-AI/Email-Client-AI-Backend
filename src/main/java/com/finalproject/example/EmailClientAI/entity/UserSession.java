package com.finalproject.example.EmailClientAI.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSession extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "google_access_token", nullable = false, length = 2048)
    private String googleAccessToken;

    @Column(name = "google_refresh_token", nullable = false, length = 2048)
    private String googleRefreshToken;

    @Column(name = "app_refresh_token", nullable = false, length = 512)
    private String appRefreshToken;        // random UUID or 256-bit

    @Column(name = "device_id", nullable = false, length = 512)
    private String deviceId;              // UUID or fingerprint

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;      // e.g. 90 days

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
}
