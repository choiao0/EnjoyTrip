package com.enjoytrip.ai.service;

import com.enjoytrip.ai.client.TravelAiClient;
import com.enjoytrip.ai.planner.ToolPlan;
import com.enjoytrip.ai.planner.TravelPlanner;
import com.enjoytrip.ai.prompt.TravelPromptBuilder;
import com.enjoytrip.ai.resource.AiResource;
import com.enjoytrip.ai.tool.AiTool;
import com.enjoytrip.ai.tool.AiToolRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TravelAiService {
    private final Map<String, AiTool> tools = new LinkedHashMap<>();
    private final TravelPlanner planner;
    private final TravelPromptBuilder promptBuilder;
    private final TravelAiClient aiClient;

    public TravelAiService(
            List<AiTool> tools,
            TravelPlanner planner,
            TravelPromptBuilder promptBuilder,
            TravelAiClient aiClient
    ) {
        tools.forEach(tool -> this.tools.put(tool.name(), tool));
        this.planner = planner;
        this.promptBuilder = promptBuilder;
        this.aiClient = aiClient;
    }

    public AiChatResponse chat(AiToolRequest request) {
        ToolPlan plan = planner.plan(request);
        List<AiResource> resources = plan.getToolNames().stream()
                .map(tools::get)
                .filter(tool -> tool != null)
                .map(tool -> tool.execute(request))
                .toList();
        String prompt = promptBuilder.build(request, resources);
        return new AiChatResponse(plan, resources, prompt, aiClient.generate(prompt, resources));
    }

    public AiResource executeSingleTool(String toolName, AiToolRequest request) {
        AiTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("등록되지 않은 Tool입니다: " + toolName);
        }
        return tool.execute(request);
    }

    public List<Map<String, String>> capabilities() {
        return tools.values().stream()
                .map(tool -> Map.of("name", tool.name(), "description", tool.description()))
                .toList();
    }

    public record AiChatResponse(
            ToolPlan plan,
            List<AiResource> resources,
            String prompt,
            String answer
    ) {
    }
}
