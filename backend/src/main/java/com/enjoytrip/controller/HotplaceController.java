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
    public ResponseEntity<List<Hotplace>> list(HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(hotplaceService.findByUser(currentUser(session).getId()));
    }

    @PostMapping
    public ResponseEntity<Hotplace> create(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Hotplace hotplace = hotplaceService.create(
                currentUser(session), name, description, address, lat, lng, imageFile);
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
