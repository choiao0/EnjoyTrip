package com.enjoytrip.model;

public class GroupTrip {
    private Long id;
    private String title;
    private String hostUserId;
    private String hostUserName;
    private String description;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getHostUserId() { return hostUserId; }
    public void setHostUserId(String hostUserId) { this.hostUserId = hostUserId; }

    public String getHostUserName() { return hostUserName; }
    public void setHostUserName(String hostUserName) { this.hostUserName = hostUserName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
