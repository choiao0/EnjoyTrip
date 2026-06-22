package com.enjoytrip.repository;

import com.enjoytrip.model.Plan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlanRepository extends AbstractJsonRepository<Plan> {
    public PlanRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {
        });
    }

    public List<Plan> findByUserId(String userId) {
        return readAll().stream()
                .filter(plan -> plan.getUserId().equals(userId))
                .sorted(Comparator.comparing(Plan::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<Plan> findById(String id) {
        return readAll().stream()
                .filter(plan -> plan.getId().equals(id))
                .findFirst();
    }

    public void save(Plan target) {
        List<Plan> plans = readAll();
        int existingIndex = -1;
        for (int i = 0; i < plans.size(); i += 1) {
            if (plans.get(i).getId().equals(target.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            plans.set(existingIndex, target);
        } else {
            plans.add(target);
        }
        writeAll(plans);
    }

    public void delete(String id) {
        List<Plan> plans = readAll();
        plans.removeIf(plan -> plan.getId().equals(id));
        writeAll(plans);
    }
}
