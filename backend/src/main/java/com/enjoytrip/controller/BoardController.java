package com.enjoytrip.controller;

import com.enjoytrip.model.BoardPost;
import com.enjoytrip.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
public class BoardController extends BaseController {

    @Autowired
    private BoardService boardService;

    @GetMapping
    public List<BoardPost> list() {
        return boardService.findAll();
    }

    @GetMapping("/{id}")
    public BoardPost detail(@PathVariable String id) {
        return boardService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BoardPost> create(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BoardPost post = boardService.create(currentUser(session), body.get("title"), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardPost> update(@PathVariable String id,
                                             @RequestBody Map<String, String> body,
                                             HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BoardPost post = boardService.update(currentUser(session), id, body.get("title"), body.get("content"));
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boardService.delete(currentUser(session), id);
        return ResponseEntity.noContent().build();
    }
}
