package com.enjoytrip.controller;

import com.enjoytrip.ai.resource.AiResource;
import com.enjoytrip.ai.service.TravelAiService;
import com.enjoytrip.ai.tool.AiToolRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiTravelController {
    private final TravelAiService travelAiService;

    public AiTravelController(TravelAiService travelAiService) {
        this.travelAiService = travelAiService;
    }

    @GetMapping("/capabilities")
    public List<Map<String, String>> capabilities() {
        return travelAiService.capabilities();
    }

    @PostMapping("/chat")
    public TravelAiService.AiChatResponse chat(@RequestBody AiToolRequest request) {
        return travelAiService.chat(request);
    }

    @PostMapping("/tools")
    public AiResource executeTool(
            @RequestParam(defaultValue = "attractionSearchTool") String name,
            @RequestBody AiToolRequest request
    ) {
        return travelAiService.executeSingleTool(name, request);
    }
}
