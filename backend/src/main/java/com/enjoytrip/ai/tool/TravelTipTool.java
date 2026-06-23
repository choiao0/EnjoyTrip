package com.enjoytrip.ai.tool;

import com.enjoytrip.ai.resource.AiResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TravelTipTool implements AiTool {
    @Override
    public String name() {
        return "travelTipTool";
    }

    @Override
    public String description() {
        return "여행 계획 작성에 필요한 준비 팁을 제공한다.";
    }

    @Override
    public AiResource execute(AiToolRequest request) {
        List<String> tips = List.of(
                "동선은 숙소 기준으로 오전과 오후 권역을 나누어 구성하세요.",
                "인기 관광지는 운영 시간과 휴무일을 먼저 확인하세요.",
                "사진 명소와 식사 장소를 함께 묶으면 이동 부담이 줄어듭니다."
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tips", tips);
        return new AiResource(name(), "여행 일정 작성을 위한 준비 팁 " + tips.size() + "개를 생성했습니다.", data);
    }
}
