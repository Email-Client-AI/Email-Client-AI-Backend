package com.finalproject.example.EmailClientAI.service;

import com.finalproject.example.EmailClientAI.entity.Email;

import java.util.concurrent.CompletableFuture;

public interface GmailService {

    /**
     * Asynchronously fetches the latest 20 emails from the Primary Inbox.
     * Used for the initial sync after login.
     */
    CompletableFuture<Void> syncInitialEmails(String googleAccessToken, String userId);

    /**
     * Fetches details for a single email and saves it to the DB.
     */
    Email fetchAndSaveEmail(String gmailId, String accessToken, String appUserId);
}
