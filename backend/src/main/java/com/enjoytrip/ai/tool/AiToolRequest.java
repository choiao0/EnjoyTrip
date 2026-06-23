package com.enjoytrip.ai.tool;

public class AiToolRequest {
    private String message;
    private Integer sidoCode;
    private Integer gugunCode;
    private Integer contentTypeId;
    private String keyword;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getSidoCode() {
        return sidoCode;
    }

    public void setSidoCode(Integer sidoCode) {
        this.sidoCode = sidoCode;
    }

    public Integer getGugunCode() {
        return gugunCode;
    }

    public void setGugunCode(Integer gugunCode) {
        this.gugunCode = gugunCode;
    }

    public Integer getContentTypeId() {
        return contentTypeId;
    }

    public void setContentTypeId(Integer contentTypeId) {
        this.contentTypeId = contentTypeId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
