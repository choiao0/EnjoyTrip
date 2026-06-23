package com.enjoytrip.repository;

import com.enjoytrip.model.Hotplace;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HotplaceRepository extends AbstractJsonRepository<Hotplace> {
    public HotplaceRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {
        });
    }

    public List<Hotplace> findAll() {
        return readAll().stream()
                .sorted(Comparator.comparing(Hotplace::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Hotplace> findById(String id) {
        return readAll().stream()
                .filter(hotplace -> hotplace.getId().equals(id))
                .findFirst();
    }

    public void save(Hotplace target) {
        List<Hotplace> hotplaces = readAll();
        hotplaces.add(target);
        writeAll(hotplaces);
    }

    public void delete(String id) {
        List<Hotplace> hotplaces = readAll();
        hotplaces.removeIf(hotplace -> hotplace.getId().equals(id));
        writeAll(hotplaces);
    }
}
