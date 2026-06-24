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

    // 메시지에서 지역명 → sidoCode 매핑
    private static final Map<String, Integer> REGION_SIDO = new LinkedHashMap<>() {{
        put("서울", 1); put("인천", 2); put("대전", 3); put("대구", 4);
        put("광주", 5); put("부산", 6); put("울산", 7); put("세종", 8);
        put("경기", 31); put("강원", 32); put("충북", 33); put("충남", 34);
        put("경북", 35); put("경남", 36); put("전북", 37); put("전남", 38);
        put("제주", 39);
    }};

    // 시/군/구 단위 지명 — sidoCode 없이 keyword로 검색
    private static final List<String> CITY_KEYWORDS = List.of(
        "경주", "수원", "춘천", "강릉", "속초", "전주", "여수", "순천", "통영",
        "거제", "남해", "안동", "포항", "구미", "창원", "진주", "거창",
        "목포", "광양", "나주", "군산", "익산", "남원", "고창", "보령",
        "공주", "부여", "천안", "아산", "홍성", "태안", "서산", "충주",
        "청주", "제천", "단양", "영월", "평창", "양양", "정선", "화천",
        "고성", "설악", "가평", "양평", "이천", "용인", "파주", "고양"
    );

    private final AttractionService attractionService;

    public AttractionSearchTool(AttractionService attractionService) {
        this.attractionService = attractionService;
    }

    @Override
    public String name() { return "attractionSearchTool"; }

    @Override
    public String description() { return "기존 관광지 검색 서비스를 Tool 형태로 호출한다."; }

    @Override
    public AiResource execute(AiToolRequest request) {
        Integer sidoCode = request.getSidoCode();
        Integer gugunCode = request.getGugunCode();
        String keyword = request.getKeyword();
        String message = request.getMessage() != null ? request.getMessage() : "";

        // sidoCode 없으면 메시지에서 광역 지역명 추출
        if (sidoCode == null) {
            for (Map.Entry<String, Integer> entry : REGION_SIDO.entrySet()) {
                if (message.contains(entry.getKey())) {
                    sidoCode = entry.getValue();
                    break;
                }
            }
        }

        // keyword 없으면 메시지에서 시/군/구 지명 추출
        if (keyword == null || keyword.isBlank()) {
            for (String city : CITY_KEYWORDS) {
                if (message.contains(city)) {
                    keyword = city;
                    break;
                }
            }
        }

        List<Attraction> attractions;
        try {
            attractions = attractionService.search(sidoCode, gugunCode, null, keyword);
        } catch (RuntimeException e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", 0);
            data.put("items", List.of());
            data.put("error", e.getMessage());
            return new AiResource(name(), "관광지 데이터베이스 조회에 실패했습니다.", data);
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
