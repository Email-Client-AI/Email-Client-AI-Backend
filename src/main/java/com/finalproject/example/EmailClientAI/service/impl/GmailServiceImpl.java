package com.finalproject.example.EmailClientAI.service.impl;

import com.finalproject.example.EmailClientAI.dto.email.GmailMessageDTO;
import com.finalproject.example.EmailClientAI.dto.email.ListGmailResponseDTO;
import com.finalproject.example.EmailClientAI.dto.email.MessageHeaderDTO;
import com.finalproject.example.EmailClientAI.dto.email.MessagePayloadDTO;
import com.finalproject.example.EmailClientAI.entity.Attachment;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailServiceImpl implements GmailService {

    private final EmailRepository emailRepository;
    private final RestTemplate restTemplate = new RestTemplate();

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
                for (ListGmailResponseDTO.GmailMessageSummary msgSummary : listDto.getMessages()) {
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

            log.info("Finished background sync for user: {}", userId);

        } catch (Exception e) {
            log.error("Critical error during initial email sync for user: ", e);
        }

        return CompletableFuture.completedFuture(null);
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
        try { return new String(Base64.getUrlDecoder().decode(encoded)); }
        catch (Exception e) { return ""; }
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
