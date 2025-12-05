package com.finalproject.example.EmailClientAI.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.example.EmailClientAI.dto.email.*;
import com.finalproject.example.EmailClientAI.entity.Attachment;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.enumeration.EmailLabel;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import com.finalproject.example.EmailClientAI.repository.UserSessionRepository;
import com.finalproject.example.EmailClientAI.service.GmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailServiceImpl implements GmailService {

    @NonFinal
    @Value("${google.topic-name}")
    String googleTopicName;

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional
    public CompletableFuture<Void> syncInitialEmails(String googleAccessToken, String userId) {
        log.info("Starting background sync of Primary Inbox for user: {}", userId);

        try {
            var listUrl = UriComponentsBuilder.fromHttpUrl("https://gmail.googleapis.com/gmail/v1/users/me/messages")
                    .queryParam("maxResults", "100")
                    .queryParam("labelIds", "INBOX")
                    // .queryParam("includeSpamTrash", "false")
                    // .queryParam("q", "category:primary")
                    .toUriString();

            var headers = new HttpHeaders();
            headers.setBearerAuth(googleAccessToken);
            var entity = new HttpEntity<>(headers);

            // Fetch the List
            ResponseEntity<ListGmailResponseDTO> response = restTemplate.exchange(
                    listUrl,
                    HttpMethod.GET,
                    entity,
                    ListGmailResponseDTO.class
            );

            var listDto = response.getBody();

            if (listDto != null && listDto.getMessages() != null) {
                log.info("Found {} emails to sync.", listDto.getMessages().size());

                // Loop through list and fetch details for each
                for (GmailMessageSummaryDTO msgSummary : listDto.getMessages()) {
                    try {
                        // We call the method internal to this class
                        // Note: Depending on proxy settings, calling this() might bypass @Transactional
                        // but since we are inside an @Async thread, we handle persistence manually or rely on repository.
                        fetchAndSaveEmail(msgSummary.getId(), googleAccessToken, userId);
                    } catch (Exception e) {
                        log.error("Failed to sync specific email: ", e);
                    }
                }
            }
            sendUserWatchRequest(googleAccessToken, userId);
            log.info("Finished background sync for user: {}", userId);

        } catch (Exception e) {
            log.error("Critical error during initial email sync for user: ", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional
    @Async
    public void sendUserWatchRequest(String googleAccessToken, String userId) {
        // 1. Define the Endpoint
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/watch";

        // 2. Prepare Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(googleAccessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 3. Prepare the Request Body (Payload)
        // Replace with your actual Project ID and Topic Name
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("topicName", googleTopicName);
        requestBody.put("labelIds",
                List.of(EmailLabel.INBOX.name(),
                        EmailLabel.SENT.name(),
                        EmailLabel.SPAM.name(),
                        EmailLabel.DRAFT.name()));
        requestBody.put("labelFilterBehavior", "INCLUDE");

        // 4. Create the Request Entity
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 5. Execute the POST Request
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // 6. Extract Data
                // Gmail returns historyId as a String in JSON to ensure precision
                var historyIdStr = (String) body.get("historyId");
                // Expiration is usually a Long (timestamp)
                Object expirationObj = body.get("expiration");

                var newHistoryId = new BigInteger(historyIdStr);

                log.info("Watch Request Successful. HistoryID: {}", newHistoryId);

                // 7. Update User in DB
                var user = userRepository.findById(UUID.fromString(userId))
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                user.setLastHistoryId(newHistoryId);

                userRepository.save(user);

            } else {
                log.error("Gmail Watch Request failed with status: {}", response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.error("Error sending watch request: {}", e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error during watch request", e);
        }
    }

    @Override
    public Email fetchAndSaveEmail(String gmailId, String accessToken, String appUserId) {

        // Fetch Details
        var url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + gmailId + "?format=full";
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        var entity = new HttpEntity<>(headers);

        ResponseEntity<GmailMessageDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity, GmailMessageDTO.class);
        var dto = response.getBody();
        if (dto == null) return null;

        // Map to Entity
        var email = Email.builder()
                .gmailEmailId(dto.getId())
                .userId(UUID.fromString(appUserId))
                .threadId(dto.getThreadId())
                .snippet(dto.getSnippet())
                .receivedDate(Instant.ofEpochMilli(dto.getInternalDate()))
                .labels(new HashSet<>(dto.getLabelIds() != null ? dto.getLabelIds() : Collections.emptyList()))
                .build();
        email = emailRepository.save(email); // Save to get emailID for attachments
        parseHeaders(dto.getPayload().getHeaders(), email);
        parsePayload(dto.getPayload(), email);
        return emailRepository.save(email);
    }

    @Override
    @Transactional
    public void processGmailWebhook(PubSubMessageDTO pubSubMessageDTO) {
        try {
            // 1. Decode the Base64 data
            String encodedData = pubSubMessageDTO.getMessage().getData();
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
            String decodedString = new String(decodedBytes);

            log.info("Received Gmail Webhook Payload: {}", decodedString);

            // 2. Parse JSON to identify User and HistoryID
            JsonNode rootNode = objectMapper.readTree(decodedString);
            var emailAddress = rootNode.path("emailAddress").asText();
            var newHistoryId = new BigInteger(rootNode.path("historyId").asText());

            // 3. Find User
            var user = userRepository.findByEmail(emailAddress)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // 4. PREPARE ACCESS TOKEN (Critical for Background Tasks)
            var userSession = userSessionRepository.findTopByUserIdOrderByExpiresAtDesc(user.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

            // 5. Check Sync Logic
            BigInteger startHistoryId = user.getLastHistoryId();

            // First time sync: just save the new ID
            if (startHistoryId == null) {
                log.info("First time sync for {}. Saving history ID.", emailAddress);
                user.setLastHistoryId(newHistoryId);
                userRepository.save(user);
                return;
            }

            if(newHistoryId.compareTo(startHistoryId) <= 0) {
                log.info("No new changes for {}. Current History ID: {}, Last Synced ID: {}", emailAddress, newHistoryId, startHistoryId);
                return;
            }

            // 6. Call Gmail History API manually
            String historyUrl = "https://gmail.googleapis.com/gmail/v1/users/me/history?startHistoryId=" + startHistoryId;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userSession.getGoogleAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<ListGmailHistoryResponseDTO> response = restTemplate.exchange(
                        historyUrl,
                        HttpMethod.GET,
                        entity,
                        ListGmailHistoryResponseDTO.class
                );

                var historyBody = response.getBody();

                if (historyBody != null && historyBody.getHistory() != null) {
                    for (GmailHistoryResponseDTO historyItem : historyBody.getHistory()) {
                        // Handle Messages Added
                        if (historyItem.getMessages() != null) {
                            for (GmailMessageSummaryDTO msgInfo : historyItem.getMessages()) {
                                String msgId = msgInfo.getId();

                                // Avoid Duplicates
                                if (!emailRepository.existsByGmailEmailIdAndUserId(msgId, user.getId())) {
                                    // REUSE your existing manual fetch method!
                                    fetchAndSaveEmail(msgId, userSession.getGoogleAccessToken(), user.getId().toString());
                                }
                            }
                        }
                    }
                }

                // 7. Update User Sync Point
                user.setLastHistoryId(historyBody.getHistoryId());
                userRepository.save(user);

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // History ID is too old (user hasn't synced in 30 days).
                    // Logic: Perform a full list sync or reset historyId to newHistoryId.
                    log.warn("History ID expired. Resetting to new ID.");
                    user.setLastHistoryId(newHistoryId);
                    userRepository.save(user);
                } else {
                    throw e;
                }
            }

        } catch (Exception e) {
            log.error("Error processing Gmail webhook", e);
            // Catch all exceptions to ensure we return 200 OK to Pub/Sub
            // otherwise Google will retry sending this message for 7 days.
        }
    }

    private void parseHeaders(List<MessageHeaderDTO> headers, Email email) {
        if (headers == null) return;
        Set<String> recipients = new HashSet<>();
        for (MessageHeaderDTO h : headers) {
            String val = h.getValue();
            switch (h.getName().toLowerCase()) {
                case "subject" -> email.setSubject(val);
                case "from" -> email.setSenderEmail(cleanEmailAddress(val));
                case "to", "cc", "bcc" -> recipients.addAll(extractEmails(val));
            }
        }
        email.setRecipientEmails(recipients);
    }

    private void parsePayload(MessagePayloadDTO payload, Email email) {
        if (payload == null) return;
        String mimeType = payload.getMimeType();

        // Attachments
        if (payload.getFilename() != null && !payload.getFilename().isEmpty()
                && payload.getBody() != null && payload.getBody().getAttachmentId() != null) {

            Attachment attachment = Attachment.builder()
                    .emailId(email.getId())
                    .email(email)
                    .filename(payload.getFilename())
                    .mimeType(mimeType)
                    .size(payload.getBody().getSize() != null ? payload.getBody().getSize().longValue() : 0L)
                    .gmailAttachmentId(payload.getBody().getAttachmentId())
                    .build();
            email.getAttachments().add(attachment);
            return;
        }

        // Body Content
        if (payload.getBody() != null && payload.getBody().getData() != null) {
            String decoded = decodeBase64(payload.getBody().getData());
            if (mimeType != null) {
                if (mimeType.contains("text/html") && email.getBodyHtml() == null) email.setBodyHtml(decoded);
                else if (mimeType.contains("text/plain") && email.getBodyText() == null) email.setBodyText(decoded);
            }
        }

        // Recursion
        if (payload.getParts() != null) {
            for (MessagePayloadDTO part : payload.getParts()) {
                parsePayload(part, email);
            }
        }
    }

    private String decodeBase64(String encoded) {
        if (encoded == null) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(encoded));
        } catch (Exception e) {
            return "";
        }
    }

    private String cleanEmailAddress(String full) {
        if (full != null && full.contains("<") && full.contains(">")) {
            return full.substring(full.indexOf("<") + 1, full.indexOf(">"));
        }
        return full;
    }

    private List<String> extractEmails(String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) return Collections.emptyList();
        return Arrays.stream(headerValue.split(",")).map(String::trim).map(this::cleanEmailAddress).toList();
    }
}
