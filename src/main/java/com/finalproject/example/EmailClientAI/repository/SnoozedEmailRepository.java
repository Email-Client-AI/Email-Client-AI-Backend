package com.finalproject.example.EmailClientAI.repository;


import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.SnoozedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SnoozedEmailRepository extends JpaRepository<SnoozedEmail, UUID> {
    Optional<SnoozedEmail> findByEmailId(UUID uuid);

    List<SnoozedEmail> findBySnoozeUntilBeforeAndSnoozeUntilIsNotNull(Instant instant);

    boolean existsByEmail(Email email);
}
