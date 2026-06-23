package com.enjoytrip.controller;

import com.enjoytrip.model.Notice;
import com.enjoytrip.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController extends BaseController {

    @Autowired
    private NoticeService noticeService;

    @GetMapping
    public List<Notice> list() {
        return noticeService.findAll();
    }

    @GetMapping("/{id}")
    public Notice detail(@PathVariable String id) {
        return noticeService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Notice> create(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Notice notice = noticeService.create(currentUser(session), body.get("title"), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(notice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notice> update(@PathVariable String id,
                                          @RequestBody Map<String, String> body,
                                          HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Notice notice = noticeService.update(currentUser(session), id, body.get("title"), body.get("content"));
        return ResponseEntity.ok(notice);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        noticeService.delete(currentUser(session), id);
        return ResponseEntity.noContent().build();
    }
}
