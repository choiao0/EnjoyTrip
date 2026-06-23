package com.enjoytrip.repository;

import com.enjoytrip.model.BoardPost;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BoardRepository extends AbstractJsonRepository<BoardPost> {
    public BoardRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {
        });
    }

    public List<BoardPost> findAll() {
        return readAll().stream()
                .sorted(Comparator.comparing(BoardPost::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<BoardPost> findById(String id) {
        return readAll().stream()
                .filter(post -> post.getId().equals(id))
                .findFirst();
    }

    public void save(BoardPost target) {
        List<BoardPost> posts = readAll();
        int existingIndex = -1;
        for (int i = 0; i < posts.size(); i += 1) {
            if (posts.get(i).getId().equals(target.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            posts.set(existingIndex, target);
        } else {
            posts.add(target);
        }
        writeAll(posts);
    }

    public void delete(String id) {
        List<BoardPost> posts = readAll();
        posts.removeIf(post -> post.getId().equals(id));
        writeAll(posts);
    }
}
