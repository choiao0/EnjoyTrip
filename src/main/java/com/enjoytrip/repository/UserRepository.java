package com.enjoytrip.repository;

import com.enjoytrip.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class UserRepository extends AbstractJsonRepository<User> {
    public UserRepository(Path filePath, ObjectMapper objectMapper) {
        super(filePath, objectMapper, new TypeReference<>() {
        });
    }

    public List<User> findAll() {
        return readAll();
    }

    public Optional<User> findById(String id) {
        return readAll().stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    public Optional<User> findByIdAndName(String id, String name) {
        return readAll().stream()
                .filter(user -> user.getId().equals(id) && user.getName().equals(name))
                .findFirst();
    }

    public void save(User target) {
        List<User> users = readAll();
        int existingIndex = -1;
        for (int i = 0; i < users.size(); i += 1) {
            if (users.get(i).getId().equals(target.getId())) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            users.set(existingIndex, target);
        } else {
            users.add(target);
        }
        writeAll(users);
    }

    public void delete(String id) {
        List<User> users = readAll();
        users.removeIf(user -> user.getId().equals(id));
        writeAll(users);
    }
}
