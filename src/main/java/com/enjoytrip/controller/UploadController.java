package com.enjoytrip.controller;

import com.enjoytrip.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class UploadController {

    @Autowired
    private AppProperties appProperties;

    @GetMapping("/uploads/{filename:.+}")
    public void serveFile(@PathVariable String filename, HttpServletResponse response) throws IOException {
        Path filePath = Path.of(appProperties.getStorageDir())
                .resolve("uploads").resolve("hotplaces").resolve(filename);
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(filePath);
        response.setContentType(contentType == null ? "application/octet-stream" : contentType);
        Files.copy(filePath, response.getOutputStream());
    }
}
