package com.enjoytrip.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String id;
    private String name;
    @JsonProperty(access = Access.WRITE_ONLY)
    private String pwHash;
    private String role = "USER";
    private String createdAt;
    private String updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPwHash() { return pwHash; }
    public void setPwHash(String pwHash) { this.pwHash = pwHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @JsonIgnore
    public boolean isAdmin() { return "ADMIN".equals(role); }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
