package com.example.aiservice.service;

import com.example.aiservice.exception.AiServiceException;
import com.example.aiservice.model.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api-key}")
    private String apiKey;

    // Keep total request time reasonable; we will retry a few times on temporary errors
    private static final Duration TIMEOUT = Duration.of(12, ChronoUnit.SECONDS);
    private static final String MODEL_PATH = "/models/gemini-2.5-flash:generateContent";
    private static final int MAX_ATTEMPTS = 3;

    public String invokeGemini(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("gemini.api-key is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", prompt))
                        )
                )
        );

        AiServiceException lastAiError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                GeminiResponse response = geminiWebClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path(MODEL_PATH)
                                .queryParam("key", apiKey)
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(status -> status.isError(), clientResponse -> {
                            int statusCode = clientResponse.statusCode().value();
                            return clientResponse.bodyToMono(String.class).map(msg -> {
                                log.error("Gemini error response: {}", msg);
                                // 429/5xx are typically temporary for this feature
                                return new AiServiceException("Gemini API error: " + msg,
                                        statusCode == 429, true);
                            });
                        })
                        .bodyToMono(GeminiResponse.class)
                        .block(TIMEOUT);

                if (response == null) {
                    throw new AiServiceException("Không nhận được phản hồi từ Gemini", false, true);
                }
                String text = response.firstText();
                if (text == null) {
                    throw new AiServiceException("Không có nội dung trả về từ Gemini", false, true);
                }
                return text.trim();
            } catch (WebClientResponseException ex) {
                log.error("Gemini HTTP error {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);

                boolean quota = ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
                boolean temporary =
                        ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ||
                        ex.getStatusCode() == HttpStatus.REQUEST_TIMEOUT ||
                        ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE ||
                        ex.getStatusCode().is5xxServerError();

                lastAiError = new AiServiceException("Gemini API error: " + ex.getStatusCode(), quota, temporary);
            } catch (AiServiceException ex) {
                lastAiError = ex;
            } catch (Exception ex) {
                log.error("Gemini invocation failed", ex);
                lastAiError = new AiServiceException("Không thể kết nối Gemini: " + ex.getMessage(), false, true);
            }

            // Retry only on temporary errors, and only if attempts remain
            if (lastAiError != null && lastAiError.isTemporary() && attempt < MAX_ATTEMPTS) {
                long backoffMs = 500L * (1L << (attempt - 1)); // 500ms, 1s, 2s
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            // non-temporary or no attempts left
            if (lastAiError != null) throw lastAiError;
        }

        throw new AiServiceException("Không thể kết nối Gemini", false, true);
    }
}


