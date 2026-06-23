package com.enjoytrip.ai.tool;

import com.enjoytrip.ai.resource.AiResource;
import com.enjoytrip.model.Attraction;
import com.enjoytrip.service.AttractionService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AttractionSearchTool implements AiTool {
    private final AttractionService attractionService;

    public AttractionSearchTool(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    @Override
    public String name() {
        return "attractionSearchTool";
    }

    @Override
    public String description() {
        return "기존 관광지 검색 서비스를 Tool 형태로 호출한다.";
    }

    @Override
    public AiResource execute(AiToolRequest request) {
        List<Attraction> attractions;
        try {
            attractions = attractionService.search(
                    request.getSidoCode(),
                    request.getGugunCode(),
                    request.getContentTypeId(),
                    request.getKeyword()
            );
        } catch (RuntimeException e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", 0);
            data.put("items", List.of());
            data.put("error", e.getMessage());
            return new AiResource(name(), "관광지 데이터베이스 조회에 실패했습니다. DB 설정을 확인하세요.", data);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", attractions.size());
        data.put("items", attractions);

        String summary = attractions.isEmpty()
                ? "조건에 맞는 관광지를 찾지 못했습니다."
                : attractions.size() + "개의 관광지 후보를 찾았습니다.";
        return new AiResource(name(), summary, data);
    }
}
