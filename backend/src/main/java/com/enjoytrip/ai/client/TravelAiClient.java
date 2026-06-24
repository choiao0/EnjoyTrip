package com.enjoytrip.ai.client;

import com.enjoytrip.ai.resource.AiResource;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TravelAiClient {

    @Value("${gms.api.url}")
    private String apiUrl;

    @Value("${gms.api.key}")
    private String apiKey;

    @Value("${gms.model}")
    private String model;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.create();
    }

    public String generate(String prompt, List<AiResource> resources) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system",
                                "content", "당신은 한국 여행 전문 AI 도우미입니다. 제공된 관광지 데이터만 근거로 답변하고, 없는 정보는 추측하지 마세요. 마크다운 형식(## 소제목, **강조**, - 목록)으로 구조적이고 실용적인 여행 정보를 제공하세요."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<String, Object> response = restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
