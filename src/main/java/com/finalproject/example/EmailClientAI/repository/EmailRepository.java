package com.finalproject.example.EmailClientAI.repository;

import com.finalproject.example.EmailClientAI.entity.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email, String> {
  Page<Email> findByMailboxId(Long mailboxId, Pageable pageable);

  Page<Email> findByMailboxIdAndUserId(Long mailboxId, String userId, Pageable pageable);
}
