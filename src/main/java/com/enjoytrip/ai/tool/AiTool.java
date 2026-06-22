package com.enjoytrip.ai.tool;

import com.enjoytrip.ai.resource.AiResource;

public interface AiTool {
    String name();

    String description();

    AiResource execute(AiToolRequest request);
}
