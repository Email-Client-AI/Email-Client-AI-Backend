package com.finalproject.example.EmailClientAI.repository;

import com.finalproject.example.EmailClientAI.entity.Mailbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailboxRepository extends JpaRepository<Mailbox, Long> {
    List<Mailbox> findByUserId(String userId);
}
