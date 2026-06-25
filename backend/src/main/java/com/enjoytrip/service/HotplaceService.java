package com.enjoytrip.service;

import com.enjoytrip.model.Hotplace;
import com.enjoytrip.model.User;
import com.enjoytrip.repository.HotplaceRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.IdGenerator;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class HotplaceService {
    private final HotplaceRepository hotplaceRepository;
    private final Path uploadRoot;

    public HotplaceService(HotplaceRepository hotplaceRepository, Path uploadRoot) {
        this.hotplaceRepository = hotplaceRepository;
        this.uploadRoot = uploadRoot;
    }

    public List<Hotplace> findByUser(String userId) {
        return hotplaceRepository.findByUserId(userId);
    }

    public Hotplace create(User user, String name, String description,
                           String address, Double lat, Double lng,
                           MultipartFile imageFile) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("장소명을 입력하세요.");
        }
        String savedImagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                Files.createDirectories(uploadRoot);
                String original = Path.of(imageFile.getOriginalFilename()).getFileName().toString();
                String safeName = IdGenerator.nextId() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path target = uploadRoot.resolve(safeName);
                try (InputStream in = imageFile.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                savedImagePath = safeName;
            } catch (IOException e) {
                throw new IllegalStateException("이미지를 저장하지 못했습니다.", e);
            }
        }
        Hotplace hotplace = new Hotplace();
        hotplace.setId(IdGenerator.nextId());
        hotplace.setUserId(user.getId());
        hotplace.setName(name.trim());
        hotplace.setDescription(description == null ? "" : description.trim());
        hotplace.setAddress(address == null ? "" : address.trim());
        hotplace.setLat(lat);
        hotplace.setLng(lng);
        hotplace.setImagePath(savedImagePath);
        hotplace.setCreatedAt(DateTimeUtil.now());
        hotplaceRepository.save(hotplace);
        return hotplace;
    }

    public Hotplace update(User user, String id, String name, String description) {
        Hotplace hotplace = hotplaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));
        if (!hotplace.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 저장한 장소만 수정할 수 있습니다.");
        }
        if (name != null && !name.isBlank()) hotplace.setName(name.trim());
        if (description != null) hotplace.setDescription(description.trim());
        hotplaceRepository.update(hotplace);
        return hotplace;
    }

    public void delete(User user, String id) {
        Hotplace hotplace = hotplaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다."));
        if (!hotplace.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 저장한 장소만 삭제할 수 있습니다.");
        }
        String imagePath = hotplace.getImagePath();
        if (imagePath != null && !imagePath.startsWith("http://") && !imagePath.startsWith("https://")) {
            try {
                Files.deleteIfExists(uploadRoot.resolve(imagePath));
            } catch (IOException ignored) {}
        }
        hotplaceRepository.delete(id);
    }
}
