package com.enjoytrip.service;

import com.enjoytrip.model.Comment;
import com.enjoytrip.model.User;
import com.enjoytrip.repository.CommentRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.IdGenerator;

import java.util.List;

public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> findByPost(String postId) {
        return commentRepository.findByPostId(postId);
    }

    public int countByPost(String postId) {
        return commentRepository.countByPostId(postId);
    }

    public Comment create(User user, String postId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력하세요.");
        }
        Comment comment = new Comment();
        comment.setId(IdGenerator.nextId());
        comment.setPostId(postId);
        comment.setAuthorId(user.getId());
        comment.setAuthorName(user.getName());
        comment.setContent(content.trim());
        comment.setCreatedAt(DateTimeUtil.now());
        commentRepository.save(comment);
        return comment;
    }

    public void delete(User user, String commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getAuthorId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(commentId);
    }
}
