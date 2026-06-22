package com.enjoytrip.ai.client;

import com.enjoytrip.ai.resource.AiResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TravelAiClient {
    public String generate(String prompt, List<AiResource> resources) {
        StringBuilder answer = new StringBuilder();
        answer.append("Tool 실행 결과를 바탕으로 정리한 여행 답변입니다.\n\n");

        for (AiResource resource : resources) {
            answer.append("- ").append(resource.getSummary()).append("\n");
            if ("attractionSearchTool".equals(resource.getName())) {
                appendAttractions(answer, resource);
            }
            if ("travelTipTool".equals(resource.getName())) {
                appendTips(answer, resource);
            }
        }

        answer.append("\n서버가 Tool 호출 순서를 관리했으며, 위 응답은 Resource 결과만 사용했습니다.");
        return answer.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendAttractions(StringBuilder answer, AiResource resource) {
        Object items = resource.getData().get("items");
        if (!(items instanceof List<?> list) || list.isEmpty()) {
            return;
        }

        answer.append("  추천 후보: ");
        list.stream().limit(3).forEach(item -> {
            try {
                Object title = item.getClass().getMethod("getTitle").invoke(item);
                answer.append(title).append(", ");
            } catch (ReflectiveOperationException ignored) {
            }
        });
        int length = answer.length();
        if (length >= 2 && answer.substring(length - 2).equals(", ")) {
            answer.delete(length - 2, length);
        }
        answer.append("\n");
    }

    @SuppressWarnings("unchecked")
    private void appendTips(StringBuilder answer, AiResource resource) {
        Object tips = resource.getData().get("tips");
        if (tips instanceof List<?> list) {
            for (Object tip : list) {
                answer.append("  · ").append(tip).append("\n");
            }
        }
    }
}
