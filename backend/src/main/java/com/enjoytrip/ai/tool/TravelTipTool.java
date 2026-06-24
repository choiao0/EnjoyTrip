package com.enjoytrip.ai.tool;

import com.enjoytrip.ai.resource.AiResource;
import org.springframework.stereotype.Component;

import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TravelTipTool implements AiTool {

    @Override
    public String name() { return "travelTipTool"; }

    @Override
    public String description() { return "여행 계획 작성에 필요한 준비 팁과 계절 정보를 제공한다."; }

    @Override
    public AiResource execute(AiToolRequest request) {
        List<String> tips = new ArrayList<>(baseTips());
        tips.addAll(seasonTips());

        String keyword = request.getKeyword() != null ? request.getKeyword() : "";
        if (keyword.contains("제주")) tips.addAll(List.of("제주는 렌터카 없이 이동이 불편합니다. 사전 예약을 권장합니다.", "제주 여행 성수기(7~8월, 12~1월)는 항공·숙박이 빠르게 마감됩니다."));
        if (keyword.contains("부산") || keyword.contains("해운대")) tips.add("부산 해운대·광안리는 여름 주말 극혼잡. 평일 방문을 추천합니다.");
        if (keyword.contains("경주")) tips.add("경주는 자전거 투어가 인기입니다. 자전거 대여소를 활용하세요.");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tips", tips);
        data.put("season", currentSeason());
        return new AiResource(name(), "여행 팁 " + tips.size() + "개와 현재 계절(" + currentSeason() + ") 정보를 제공합니다.", data);
    }

    private List<String> baseTips() {
        return List.of(
                "동선은 숙소 기준으로 오전/오후 권역을 나눠 구성하세요.",
                "인기 관광지는 운영 시간과 휴무일을 미리 확인하세요.",
                "공휴일·연휴에는 인파가 2~3배 몰립니다. 평일 방문을 권장합니다.",
                "식사 명소와 관광지를 같은 동선에 묶으면 이동이 줄어듭니다.",
                "여행 하루 전 목적지의 날씨를 확인하고 복장을 준비하세요.",
                "주요 관광지 주변 주차는 혼잡합니다. 대중교통 또는 도보 활용을 권장합니다."
        );
    }

    private String currentSeason() {
        Month m = MonthDay.now().getMonth();
        return switch (m) {
            case MARCH, APRIL, MAY -> "봄";
            case JUNE, JULY, AUGUST -> "여름";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "가을";
            default -> "겨울";
        };
    }

    private List<String> seasonTips() {
        return switch (currentSeason()) {
            case "봄" -> List.of("3~5월은 벚꽃·유채꽃 시즌입니다. 개화 시기에 맞춰 방문하세요.", "꽃구경 명소는 주말 극혼잡이므로 이른 아침 방문을 추천합니다.");
            case "여름" -> List.of("7~8월 해수욕장은 성수기입니다. 오전 일찍 자리를 잡으세요.", "폭염 대비 자외선 차단제와 충분한 수분 섭취가 필요합니다.");
            case "가을" -> List.of("9~11월은 단풍 절정 시즌입니다. 산악 관광지 방문에 최적입니다.", "국립공원은 단풍철 주말에 예약제 또는 차량 통제가 있을 수 있습니다.");
            case "겨울" -> List.of("겨울 여행지로 스키장·온천·눈꽃 명소를 함께 계획하세요.", "산간 지역은 결빙 구간이 있으니 안전 운전에 주의하세요.");
            default -> List.of();
        };
    }
}
