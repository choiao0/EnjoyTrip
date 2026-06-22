package com.enjoytrip.ai.resource;

import java.util.Map;

public class AiResource {
    private String name;
    private String summary;
    private Map<String, Object> data;

    public AiResource() {
    }

    public AiResource(String name, String summary, Map<String, Object> data) {
        this.name = name;
        this.summary = summary;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
