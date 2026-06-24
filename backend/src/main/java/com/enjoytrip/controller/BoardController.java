package com.enjoytrip.controller;

import com.enjoytrip.model.BoardPost;
import com.enjoytrip.model.Comment;
import com.enjoytrip.service.BoardService;
import com.enjoytrip.service.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
public class BoardController extends BaseController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private CommentService commentService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return boardService.findAll().stream().map(post -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", post.getId());
            map.put("title", post.getTitle());
            map.put("authorId", post.getAuthorId());
            map.put("authorName", post.getAuthorName());
            map.put("createdAt", post.getCreatedAt());
            map.put("commentCount", commentService.countByPost(post.getId()));
            return map;
        }).toList();
    }

    @GetMapping("/{id}")
    public BoardPost detail(@PathVariable String id) {
        return boardService.findById(id);
    }

    @PostMapping
    public ResponseEntity<BoardPost> create(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardPost post = boardService.create(currentUser(session), body.get("title"), body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardPost> update(@PathVariable String id,
                                             @RequestBody Map<String, String> body,
                                             HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BoardPost post = boardService.update(currentUser(session), id, body.get("title"), body.get("content"));
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boardService.delete(currentUser(session), id);
        return ResponseEntity.noContent().build();
    }

    // ── 댓글 ─────────────────────────────────────────────────────

    @GetMapping("/{id}/comments")
    public List<Comment> comments(@PathVariable String id) {
        return commentService.findByPost(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable String id,
                                               @RequestBody Map<String, String> body,
                                               HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Comment comment = commentService.create(currentUser(session), id, body.get("content"));
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable String id,
                                               @PathVariable String commentId,
                                               HttpSession session) {
        if (!isLoggedIn(session)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        commentService.delete(currentUser(session), commentId);
        return ResponseEntity.noContent().build();
    }
}
