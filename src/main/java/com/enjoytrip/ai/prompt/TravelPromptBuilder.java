package com.enjoytrip.ai.prompt;

import com.enjoytrip.ai.resource.AiResource;
import com.enjoytrip.ai.tool.AiToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TravelPromptBuilder {
    public String build(AiToolRequest request, List<AiResource> resources) {
        String resourceText = resources.stream()
                .map(resource -> "- " + resource.getName() + ": " + resource.getSummary())
                .collect(Collectors.joining("\n"));

        return """
                사용자의 여행 질문에 답변한다.
                추측하지 말고 Tool 실행 결과(Resource)에 포함된 정보만 근거로 사용한다.

                [사용자 요청]
                %s

                [Resource]
                %s
                """.formatted(request.getMessage(), resourceText);
    }
}
