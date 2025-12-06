package com.finalproject.example.EmailClientAI.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.example.EmailClientAI.dto.email.*;
import com.finalproject.example.EmailClientAI.entity.Attachment;
import com.finalproject.example.EmailClientAI.entity.Email;
import com.finalproject.example.EmailClientAI.entity.User;
import com.finalproject.example.EmailClientAI.enumeration.EmailLabel;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import com.finalproject.example.EmailClientAI.repository.EmailRepository;
import com.finalproject.example.EmailClientAI.repository.UserRepository;
import com.finalproject.example.EmailClientAI.service.GmailService;
import com.finalproject.example.EmailClientAI.service.UserService;
import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
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

import java.io.ByteArrayOutputStream;
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
    private final UserService userService;
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
            // 1. Decode payload
            String encodedData = pubSubMessageDTO.getMessage().getData();
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData);
            String decodedString = new String(decodedBytes);

            log.info("Received Gmail Webhook Payload: {}", decodedString);

            // 2. Parse JSON
            JsonNode rootNode = objectMapper.readTree(decodedString);
            var emailAddress = rootNode.path("emailAddress").asText();
            var incomingHistoryId = new BigInteger(rootNode.path("historyId").asText());

            // 3. Find User
            var user = userRepository.findByEmail(emailAddress)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // 4. Get Access Token
            var userSession = userService.findActiveSession(user.getId());

            // 5. Optimization Check
            BigInteger currentDbHistoryId = user.getLastHistoryId();

            if (currentDbHistoryId == null) {
                // First time ever: just set the ID and return (or trigger full sync)
                log.info("First time sync for {}. Saving history ID.", emailAddress);
                user.setLastHistoryId(incomingHistoryId);
                userRepository.save(user);
                return;
            }

            // If DB is already ahead of or equal to the notification, ignore it
            if (incomingHistoryId.compareTo(currentDbHistoryId) <= 0) {
                log.info("Stale notification. DB: {}, Incoming: {}. Skipping.", currentDbHistoryId, incomingHistoryId);
                return;
            }

            // 6. DELEGATE TO THE NEW FUNCTION
            syncEmailsFromHistoryId(user, userSession.getGoogleAccessToken(), currentDbHistoryId, incomingHistoryId);

        } catch (Exception e) {
            log.error("Error processing Gmail webhook", e);
            // Swallowed to ensure 200 OK return to Pub/Sub
        }
    }

    @Override
    @Transactional
    public void syncEmailsFromHistoryId(User user, String accessToken, BigInteger startHistoryId, BigInteger fallbackHistoryId) {
        String historyUrl = "https://gmail.googleapis.com/gmail/v1/users/me/history?startHistoryId=" + startHistoryId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // 1. Execute Manual HTTP Request
            ResponseEntity<ListGmailHistoryResponseDTO> response = restTemplate.exchange(
                    historyUrl,
                    HttpMethod.GET,
                    entity,
                    ListGmailHistoryResponseDTO.class
            );

            var historyBody = response.getBody();

            // 2. Process the History List
            if (historyBody != null && historyBody.getHistory() != null) {
                for (GmailHistoryResponseDTO historyItem : historyBody.getHistory()) {
                    // Handle Messages Added
                    // Note: Ensure your DTO structure matches Gmail API ("messagesAdded" array)
                    if (historyItem.getMessages() != null) {
                        for (GmailMessageSummaryDTO msgInfo : historyItem.getMessages()) {
                            String msgId = msgInfo.getId();

                            // Avoid Duplicates
                            if (!emailRepository.existsByGmailEmailIdAndUserId(msgId, user.getId())) {
                                fetchAndSaveEmail(msgId, accessToken, user.getId().toString());
                            }
                        }
                    }
                    // TODO: You can add logic for 'labelsAdded' or 'messagesDeleted' here
                }
            }

            // 3. Update User Sync Point (Crucial: Use the ID from the API response)
            if (historyBody != null && historyBody.getHistoryId() != null) {
                user.setLastHistoryId(historyBody.getHistoryId());
                userRepository.save(user);
                log.info("Sync complete. Updated History ID to: {}", historyBody.getHistoryId());
            }

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // 4. Handle Expired History (User hasn't synced in ~30 days)
                log.warn("History ID {} not found (expired). Resetting to fallback ID: {}", startHistoryId, fallbackHistoryId);

                // Option A: Reset to the ID we just received from Pub/Sub (Fast, but misses data in the gap)
                user.setLastHistoryId(fallbackHistoryId);
                userRepository.save(user);

                // Option B (Better): Trigger a full syncInitialEmails() here
                // syncInitialEmails(accessToken, user.getId().toString());
            } else {
                throw e;
            }
        }
    }

    @Override
    public void sendEmail(String googleAccessToken, GmailSendRequestDTO request) {
        try {
            // 1. Create the MIME Message (RFC 2822)
            var mimeMessage = createMimeMessage(request, googleAccessToken);

            // 2. Convert MIME to ByteArray
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            mimeMessage.writeTo(buffer);
            byte[] rawBytes = buffer.toByteArray();

            // 3. Encode to URL-Safe Base64 (Required by Gmail API)
            String encodedEmail = Base64.getUrlEncoder().encodeToString(rawBytes);

            // 4. Prepare JSON Payload
            Map<String, String> payload = new HashMap<>();
            payload.put("raw", encodedEmail);

            // Optimization: If replying, strictly associate with the thread
            if (request.getReplyToEmailId() != null) {
                var email = emailRepository.findById(UUID.fromString(request.getReplyToEmailId()))
                        .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
                String threadId = email.getThreadId();
                if (threadId != null) {
                    payload.put("threadId", threadId);
                }
            }

            // 5. Execute HTTP POST
            String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(googleAccessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Email sent successfully!");
            } else {
                log.error("Failed to send email:", response);
                throw new AppException(ErrorCode.SEND_EMAIL_FAILURE);
            }

        } catch (Exception e) {
            log.error("Failed to send email", e);
            throw new AppException(ErrorCode.SEND_EMAIL_FAILURE);
        }
    }

    @Override
    public AttachmentDownloadDTO downloadAttachment(UUID emailId ,UUID attachmentId, String accessToken) {
        Email email = emailRepository.findById(emailId)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        // Find the specific attachment in the email's list
        Attachment attachmentMeta = email.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND));

        // 2. Call Gmail API to fetch the Data
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
                + email.getGmailEmailId()
                + "/attachments/" + attachmentMeta.getGmailAttachmentId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.get("data") != null) {
                String base64Data = (String) body.get("data");

                // 3. Decode URL-Safe Base64
                byte[] fileBytes = Base64.getUrlDecoder().decode(base64Data);

                return AttachmentDownloadDTO.builder()
                        .filename(attachmentMeta.getFileName())
                        .mimeType(attachmentMeta.getMimeType())
                        .data(fileBytes)
                        .build();
            } else {
                throw new AppException(ErrorCode.GMAIL_API_ERROR);
            }
        } catch (Exception e) {
            log.error("Failed to download attachment", e);
            throw new AppException(ErrorCode.GMAIL_API_ERROR);
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
                    .fileName(payload.getFilename())
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

    /**
     * Helper to construct the MIME message using JavaMail.
     * Handles Address parsing, Subject encoding, HTML body, Attachments (Forward), and Reply Headers.
     */
    private MimeMessage createMimeMessage(GmailSendRequestDTO request, String accessToken) throws MessagingException {
        Session session = Session.getDefaultInstance(new Properties(), null);
        MimeMessage email = new MimeMessage(session);

        // 1. Set Recipients
        if (request.getTo() != null) {
            for (String to : request.getTo()) email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        }
        if (request.getCc() != null) {
            for (String cc : request.getCc()) email.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(cc));
        }
        if (request.getBcc() != null) {
            for (String bcc : request.getBcc()) email.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bcc));
        }

        // 2. Set Subject
        email.setSubject(request.getSubject(), "UTF-8");

        // 3. PREPARE CONTENT (Multipart is needed for Body + Attachments)
        MimeMultipart multipart = new MimeMultipart();

        // 3a. Add the HTML Body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(request.getBodyHtml(), "text/html; charset=utf-8");
        multipart.addBodyPart(textPart);

        // 4. HANDLE FORWARDING (Re-attach files)
        // Check if this is a Forward request (assuming your DTO has this field)
        if (request.getForwardEmailId() != null) {
            var originalEmail = emailRepository.findById(UUID.fromString(request.getForwardEmailId()))
                    .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

            if (originalEmail.getAttachments() != null) {
                for (Attachment dbAttachment : originalEmail.getAttachments()) {
                    // Fetch the actual file bytes from Google
                    byte[] fileBytes = fetchAttachmentBytes(originalEmail.getGmailEmailId(), dbAttachment.getGmailAttachmentId(), accessToken);

                    if (fileBytes.length > 0) {
                        // Create attachment part
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        ByteArrayDataSource dataSource = new ByteArrayDataSource(fileBytes, dbAttachment.getMimeType());

                        attachmentPart.setDataHandler(new DataHandler(dataSource));
                        attachmentPart.setFileName(dbAttachment.getFileName());

                        multipart.addBodyPart(attachmentPart);
                    }
                }
            }
        }

        // 5. Set the multipart content to the email
        email.setContent(multipart);

        // 6. HANDLE REPLY HEADERS (Crucial for Threading)
        // Only set threading headers if it is a Reply AND NOT a Forward
        if (request.getReplyToEmailId() != null && request.getForwardEmailId() == null) {
            var emailEntity = emailRepository.findById(UUID.fromString(request.getReplyToEmailId()))
                    .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

            // Fetch threading headers from Gmail API
            Map<String, String> originalHeaders = fetchThreadingHeaders(emailEntity.getGmailEmailId(), accessToken);

            String originalMessageId = originalHeaders.get("Message-Id");
            String originalReferences = originalHeaders.get("References");

            if (originalMessageId != null) {
                // The 'In-Reply-To' header tells email clients which specific message this is replying to
                email.setHeader("In-Reply-To", originalMessageId);

                // The 'References' header is a chain of all previous IDs. We append the new one.
                String newReferences = (originalReferences != null ? originalReferences + " " : "") + originalMessageId;
                email.setHeader("References", newReferences);
            }
        }

        return email;
    }



    /**
     * Helper to fetch specific headers (Message-Id, References) from Gmail API.
     * We use 'format=metadata' to be lightweight (don't download body).
     */
    private Map<String, String> fetchThreadingHeaders(String gmailMessageId, String accessToken) {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + gmailMessageId + "?format=metadata";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, String> result = new HashMap<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            // Extract ThreadId while we are here
            if (root.has("threadId")) {
                result.put("threadId", root.get("threadId").asText());
            }

            // Extract Headers
            JsonNode headersNode = root.path("payload").path("headers");
            if (headersNode.isArray()) {
                for (JsonNode header : headersNode) {
                    String name = header.path("name").asText();
                    if ("Message-Id".equalsIgnoreCase(name)) {
                        result.put("Message-Id", header.path("value").asText());
                    } else if ("References".equalsIgnoreCase(name)) {
                        result.put("References", header.path("value").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not fetch threading headers for message: {}", gmailMessageId, e);
            throw new AppException(ErrorCode.SEND_EMAIL_FAILURE);
        }
        return result;
    }

    /**
     * Downloads attachment binary data from Gmail API.
     */
    private byte[] fetchAttachmentBytes(String messageId, String attachmentId, String accessToken) {
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId + "/attachments/" + attachmentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("data") != null) {
                String base64Data = (String) response.getBody().get("data");
                // Gmail API uses URL-Safe Base64
                return Base64.getUrlDecoder().decode(base64Data);
            }
        } catch (Exception e) {
            log.error("Failed to fetch attachment {} for message {}", attachmentId, messageId, e);
        }
        return new byte[0];
    }
}
