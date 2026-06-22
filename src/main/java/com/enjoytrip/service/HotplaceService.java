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

    public List<Hotplace> findAll() {
        return hotplaceRepository.findAll();
    }

    public Hotplace create(User user, String name, String description, MultipartFile imageFile) {
        if (name == null || name.isBlank() || description == null || description.isBlank()) {
            throw new IllegalArgumentException("장소명과 상세 설명을 모두 입력하세요.");
        }
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일을 선택하세요.");
        }
        try {
            Files.createDirectories(uploadRoot);
            String original = Path.of(imageFile.getOriginalFilename()).getFileName().toString();
            String safeName = IdGenerator.nextId() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = uploadRoot.resolve(safeName);
            try (InputStream in = imageFile.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            Hotplace hotplace = new Hotplace();
            hotplace.setId(IdGenerator.nextId());
            hotplace.setUserId(user.getId());
            hotplace.setUserName(user.getName());
            hotplace.setName(name.trim());
            hotplace.setDescription(description.trim());
            hotplace.setImagePath(safeName);
            hotplace.setCreatedAt(DateTimeUtil.now());
            hotplaceRepository.save(hotplace);
            return hotplace;
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", e);
        }
    }

    public void delete(User user, String id) {
        Hotplace hotplace = hotplaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("핫플레이스를 찾을 수 없습니다."));
        if (!hotplace.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("본인이 등록한 핫플레이스만 삭제할 수 있습니다.");
        }
        try {
            Files.deleteIfExists(uploadRoot.resolve(hotplace.getImagePath()));
        } catch (IOException ignored) {
        }
        hotplaceRepository.delete(id);
    }
}
