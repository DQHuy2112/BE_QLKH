package com.example.inventory_service.client;

import com.example.inventory_service.dto.UpdateReceiptMetadataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://ai-service}")
    private String aiServiceUrl;

    public void updateReceiptMetadata(UpdateReceiptMetadataRequest request) {
        try {
            String url = aiServiceUrl + "/api/ai/receipt-ocr/update-metadata";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UpdateReceiptMetadataRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception ex) {
            log.warn("Failed to update receipt metadata to AI service: {}", ex.getMessage());
        }
    }
}

