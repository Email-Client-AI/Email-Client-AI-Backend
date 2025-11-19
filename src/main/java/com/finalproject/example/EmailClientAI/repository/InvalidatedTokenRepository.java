package com.finalproject.example.EmailClientAI.repository;

import com.finalproject.example.EmailClientAI.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM InvalidatedToken t " +
            "WHERE t.accessId = :id OR t.refreshId = :id")
    boolean existsByAccessIdOrRefreshId(String id);

    void deleteAllByExpirationTimeBefore(LocalDateTime time);
}
