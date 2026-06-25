package com.enjoytrip.ai.prompt;

import com.enjoytrip.ai.resource.AiResource;
import com.enjoytrip.ai.tool.AiToolRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TravelPromptBuilder {

    public String build(AiToolRequest request, List<AiResource> resources) {
        StringBuilder sb = new StringBuilder();
        sb.append("[수집된 데이터]\n");

        for (AiResource resource : resources) {
            sb.append("\n## ").append(resource.getName()).append("\n");
            sb.append(resource.getSummary()).append("\n");

            Map<String, Object> data = resource.getData();
            if (data == null) continue;

            // 관광지 목록 — 상위 12개 이름·주소·개요 포함
            Object items = data.get("items");
            if (items instanceof List<?> list && !list.isEmpty()) {
                int limit = Math.min(list.size(), 12);
                for (int i = 0; i < limit; i++) {
                    if (!(list.get(i) instanceof Map<?, ?> m)) continue;
                    String title   = str(m, "title");
                    String address = str(m, "address");
                    String overview = str(m, "overview");
                    sb.append("- **").append(title).append("**");
                    if (!address.isBlank()) sb.append(" (").append(address).append(")");
                    if (!overview.isBlank()) {
                        String brief = overview.length() > 80 ? overview.substring(0, 80) + "…" : overview;
                        sb.append(": ").append(brief);
                    }
                    sb.append("\n");
                }
            }

            // 팁 목록
            Object tips = data.get("tips");
            if (tips instanceof List<?> tipList) {
                tipList.forEach(t -> sb.append("- ").append(t).append("\n"));
            }
        }

        return """
                [역할]
                당신은 한국 여행 전문 AI 도우미입니다.
                아래 [수집된 데이터]에 포함된 관광지 목록을 적극 활용해서 추천과 일정을 구성하세요.
                데이터에 관광지가 있다면 반드시 그 중에서 골라 구체적으로 추천해야 합니다.
                데이터가 부족한 경우에도 제공된 관광지를 바탕으로 최선의 일정을 제안하세요.

                [답변 형식]
                - 마크다운을 사용해 구조적으로 작성하세요 (## 소제목, **강조**, - 목록)
                - 관광지 3~5곳을 구체적으로 추천하고 각 특징을 한 줄로 설명하세요
                - 오전/오후로 나눈 하루 일정을 반드시 제안하세요
                - 답변은 한국어로, 친절하고 실용적으로 작성하세요

                [사용자 질문]
                %s

                %s
                """.formatted(request.getMessage(), sb.toString());
    }

    private String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s.trim() : "";
    }
}
