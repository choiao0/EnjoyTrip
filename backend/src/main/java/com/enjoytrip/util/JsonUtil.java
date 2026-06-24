package com.enjoytrip.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {
    private JsonUtil() {
    }

    /** HTTP 응답용: @JsonProperty(access=WRITE_ONLY) 적용 → pwHash 응답 제외 */
    public static ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    /** 파일 저장용: 어노테이션 무시 → pwHash 포함 저장 */
    public static ObjectMapper fileObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.disable(MapperFeature.USE_ANNOTATIONS);
        return om;
    }
}
