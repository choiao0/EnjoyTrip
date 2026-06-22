package com.enjoytrip.repository;

import com.enjoytrip.model.Notice;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class NoticeRepository extends AbstractJsonRepository<Notice> {
    public NoticeRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {
        });
    }

    public List<Notice> findAll() {
        return readAll().stream()
                .sorted(Comparator.comparing(Notice::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Notice> findById(String id) {
        return readAll().stream()
                .filter(notice -> notice.getId().equals(id))
                .findFirst();
    }

    public void save(Notice target) {
        List<Notice> notices = readAll();
        int existingIndex = -1;
        for (int i = 0; i < notices.size(); i += 1) {
            if (notices.get(i).getId().equals(target.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            notices.set(existingIndex, target);
        } else {
            notices.add(target);
        }
        writeAll(notices);
    }

    public void delete(String id) {
        List<Notice> notices = readAll();
        notices.removeIf(notice -> notice.getId().equals(id));
        writeAll(notices);
    }
}
