package com.finalproject.example.EmailClientAI.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finalproject.example.EmailClientAI.exception.AppException;
import com.finalproject.example.EmailClientAI.exception.ErrorCode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PythonEmailClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_API_URL = "http://127.0.0.1:8000"; // Adjust if deployed elsewhere

    // --- DTOs for Python Communication ---

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IngestRequest {
        @JsonProperty("source_id")
        private String sourceId; // UUID as String
        private String content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchResponse {
        private String id; // UUID
        private Double score;
        private String summary;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SuggestionResponse {
        private String text;
        private String type; // 'history' or 'keyword'
    }

    // --- METHODS ---

    public void ingestEmail(UUID emailId, String content) {
        try {
            String url = PYTHON_API_URL + "/ingest";
            IngestRequest request = new IngestRequest(emailId.toString(), content);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<IngestRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, Map.class);
            log.info("Successfully ingested email {} to Vector DB", emailId);
        } catch (Exception e) {
            log.error("Failed to ingest email {} to Python Service: ", emailId, e);
            throw new AppException(ErrorCode.ERROR_IN_INGRESTING_EMAIL);
        }
    }

    public List<SearchResponse> search(String query) {
        try {
            String url = PYTHON_API_URL + "/search?query=" + query;
            ResponseEntity<SearchResponse[]> response = restTemplate.getForEntity(url, SearchResponse[].class);
            if (response.getBody() != null) {
                return List.of(response.getBody());
            }
        } catch (Exception e) {
            log.error("Python Search failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return Collections.emptyList();
    }

    public List<SuggestionResponse> suggest(String query) {
        try {
            String url = PYTHON_API_URL + "/suggest?q=" + query;
            ResponseEntity<SuggestionResponse[]> response = restTemplate.getForEntity(url, SuggestionResponse[].class);
            if (response.getBody() != null) {
                return List.of(response.getBody());
            }
        } catch (Exception e) {
            log.error("Python Suggest failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return Collections.emptyList();
    }

    public String summarize(String content) {
        try {
            String url = PYTHON_API_URL + "/summarize";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> request = Map.of("content", content);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() != null && response.getBody().get("summary") != null) {
                return response.getBody().get("summary").toString();
            }
        } catch (Exception e) {
            log.error("Python Summarize failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        return "";
    }
}