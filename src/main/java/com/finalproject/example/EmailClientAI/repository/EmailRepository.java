package com.finalproject.example.EmailClientAI.repository;

import com.finalproject.example.EmailClientAI.entity.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, UUID>, JpaSpecificationExecutor<Email> {
    Optional<Email> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByGmailEmailIdAndUserId(String gmailMessageId, UUID userId);

    List<Email> findByThreadIdAndUserId(String threadId, UUID userId);
}
