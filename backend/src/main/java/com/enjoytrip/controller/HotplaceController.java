package com.enjoytrip.controller;

import com.enjoytrip.model.Hotplace;
import com.enjoytrip.service.HotplaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/hotplaces")
public class HotplaceController extends BaseController {

    @Autowired
    private HotplaceService hotplaceService;

    @GetMapping
    public List<Hotplace> list() {
        return hotplaceService.findAll();
    }

    @PostMapping
    public ResponseEntity<Hotplace> create(@RequestParam String name,
                                            @RequestParam String description,
                                            @RequestParam("image") MultipartFile imageFile,
                                            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Hotplace hotplace = hotplaceService.create(currentUser(session), name, description, imageFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotplace);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        hotplaceService.delete(currentUser(session), id);
        return ResponseEntity.noContent().build();
    }
}
