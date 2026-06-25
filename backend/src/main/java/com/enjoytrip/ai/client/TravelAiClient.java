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
                        Map.of("role", "developer",
                                "content", "당신은 한국 여행 전문 AI 도우미입니다. 제공된 관광지 목록을 적극 활용해 추천과 일정을 구성하세요. 관광지가 있으면 반드시 그 중에서 골라 구체적으로 추천하고, 오전/오후로 나눈 일정을 제안하세요. 마크다운 형식으로 구조적이고 실용적으로 작성하세요. 관광지·음식·일정 등 내용과 어울리는 이모지를 소제목과 주요 항목에 적절히 사용하세요. 답변 마지막에 '원하시면', '다음에는', '추가로 도움이 필요하시면' 등의 후속 안내 문구는 절대 넣지 마세요."),
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
