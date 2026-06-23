package com.enjoytrip.ai.tool;

import com.enjoytrip.ai.resource.AiResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WeatherTool implements AiTool {
    @Override
    public String name() {
        return "weatherTool";
    }

    @Override
    public String description() {
        return "지역 코드와 키워드를 기준으로 여행 준비용 날씨 가이드를 제공한다.";
    }

    @Override
    public AiResource execute(AiToolRequest request) {
        int seed = Math.abs((String.valueOf(request.getSidoCode()) + request.getKeyword()).hashCode());
        String condition = switch (seed % 4) {
            case 0 -> "맑음";
            case 1 -> "구름 많음";
            case 2 -> "흐림";
            default -> "가벼운 비 가능";
        };
        int temperature = 18 + seed % 12;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("condition", condition);
        data.put("temperature", temperature);
        data.put("source", "local-demo-weather");

        return new AiResource(
                name(),
                "예상 날씨는 " + condition + ", 기온은 약 " + temperature + "도입니다.",
                data
        );
    }
}
