package com.enjoytrip.ai.planner;

import java.util.List;

public class ToolPlan {
    private String intent;
    private List<String> toolNames;

    public ToolPlan() {
    }

    public ToolPlan(String intent, List<String> toolNames) {
        this.intent = intent;
        this.toolNames = toolNames;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getToolNames() {
        return toolNames;
    }

    public void setToolNames(List<String> toolNames) {
        this.toolNames = toolNames;
    }
}
