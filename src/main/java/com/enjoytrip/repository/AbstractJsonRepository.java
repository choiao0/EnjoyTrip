package com.enjoytrip.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractJsonRepository<T> {
    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final TypeReference<List<T>> typeReference;

    protected AbstractJsonRepository(Path filePath, ObjectMapper objectMapper, TypeReference<List<T>> typeReference) {
        this.filePath = filePath;
        this.objectMapper = objectMapper;
        this.typeReference = typeReference;
        initializeFile();
    }

    protected synchronized List<T> readAll() {
        try {
            if (Files.size(filePath) == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(filePath.toFile(), typeReference);
        } catch (IOException e) {
            throw new IllegalStateException(filePath + "를 읽을 수 없습니다.", e);
        }
    }

    protected synchronized void writeAll(List<T> items) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), items);
        } catch (IOException e) {
            throw new IllegalStateException(filePath + "를 저장할 수 없습니다.", e);
        }
    }

    private void initializeFile() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                Files.writeString(filePath, "[]", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException(filePath + " 초기화에 실패했습니다.", e);
        }
    }
}
