package com.enjoytrip.repository;

import com.enjoytrip.model.Comment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommentRepository extends AbstractJsonRepository<Comment> {
    public CommentRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {});
    }

    public List<Comment> findByPostId(String postId) {
        return readAll().stream()
                .filter(c -> postId.equals(c.getPostId()))
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toList());
    }

    public int countByPostId(String postId) {
        return (int) readAll().stream().filter(c -> postId.equals(c.getPostId())).count();
    }

    public Optional<Comment> findById(String id) {
        return readAll().stream().filter(c -> id.equals(c.getId())).findFirst();
    }

    public void save(Comment comment) {
        List<Comment> all = readAll();
        all.add(comment);
        writeAll(all);
    }

    public void delete(String id) {
        List<Comment> all = readAll();
        all.removeIf(c -> id.equals(c.getId()));
        writeAll(all);
    }

    public void deleteByPostId(String postId) {
        List<Comment> all = readAll();
        all.removeIf(c -> postId.equals(c.getPostId()));
        writeAll(all);
    }
}
