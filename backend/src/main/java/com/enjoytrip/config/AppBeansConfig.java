package com.enjoytrip.config;

import com.enjoytrip.repository.BoardRepository;
import com.enjoytrip.repository.HotplaceRepository;
import com.enjoytrip.repository.NoticeRepository;
import com.enjoytrip.repository.PlanRepository;
import com.enjoytrip.repository.UserRepository;
import com.enjoytrip.service.BoardService;
import com.enjoytrip.service.HotplaceService;
import com.enjoytrip.service.NoticeService;
import com.enjoytrip.service.PlanService;
import com.enjoytrip.service.UserService;
import com.enjoytrip.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class AppBeansConfig {

    @Autowired
    private AppProperties appProperties;

    @Bean
    public ObjectMapper objectMapper() {
        return JsonUtil.objectMapper();
    }

    @Bean
    public Path storageRoot() throws IOException {
        Path root = Path.of(appProperties.getStorageDir());
        Files.createDirectories(root.resolve("data"));
        Files.createDirectories(root.resolve("uploads").resolve("hotplaces"));
        return root;
    }

    @Bean
    public UserRepository userRepository(ObjectMapper objectMapper, Path storageRoot) {
        return new UserRepository(storageRoot.resolve("data").resolve("users.json"), objectMapper);
    }

    @Bean
    public PlanRepository planRepository(ObjectMapper objectMapper, Path storageRoot) {
        return new PlanRepository(storageRoot.resolve("data").resolve("plans.json"), objectMapper);
    }

    @Bean
    public HotplaceRepository hotplaceRepository(ObjectMapper objectMapper, Path storageRoot) {
        return new HotplaceRepository(storageRoot.resolve("data").resolve("hotplaces.json"), objectMapper);
    }

    @Bean
    public BoardRepository boardRepository(ObjectMapper objectMapper, Path storageRoot) {
        return new BoardRepository(storageRoot.resolve("data").resolve("boards.json"), objectMapper);
    }

    @Bean
    public NoticeRepository noticeRepository(ObjectMapper objectMapper, Path storageRoot) {
        return new NoticeRepository(storageRoot.resolve("data").resolve("notices.json"), objectMapper);
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public PlanService planService(PlanRepository planRepository) {
        return new PlanService(planRepository);
    }

    @Bean
    public HotplaceService hotplaceService(HotplaceRepository hotplaceRepository, Path storageRoot) {
        return new HotplaceService(hotplaceRepository, storageRoot.resolve("uploads").resolve("hotplaces"));
    }

    @Bean
    public BoardService boardService(BoardRepository boardRepository) {
        return new BoardService(boardRepository);
    }

    @Bean
    public NoticeService noticeService(NoticeRepository noticeRepository) {
        return new NoticeService(noticeRepository);
    }
}
