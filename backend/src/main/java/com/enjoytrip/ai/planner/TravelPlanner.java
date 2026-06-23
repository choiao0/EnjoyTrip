package com.enjoytrip.ai.planner;

import com.enjoytrip.ai.tool.AiToolRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TravelPlanner {
    public ToolPlan plan(AiToolRequest request) {
        String message = request.getMessage() == null ? "" : request.getMessage();
        List<String> tools = new ArrayList<>();

        tools.add("attractionSearchTool");
        if (message.contains("날씨") || message.contains("준비") || message.contains("추천")) {
            tools.add("weatherTool");
        }
        if (message.contains("일정") || message.contains("코스") || message.contains("팁") || message.contains("추천")) {
            tools.add("travelTipTool");
        }

        String intent = tools.size() == 1 ? "single-tool-search" : "multi-tool-travel-recommendation";
        return new ToolPlan(intent, tools);
    }
}
