package com.enjoytrip.service;

import com.enjoytrip.model.BoardPost;
import com.enjoytrip.model.User;
import com.enjoytrip.repository.BoardRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.IdGenerator;

import java.util.List;

public class BoardService {
    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    public List<BoardPost> findAll() {
        return boardRepository.findAll();
    }

    public BoardPost findById(String id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    public BoardPost create(User user, String title, String content) {
        validate(title, content);
        BoardPost post = new BoardPost();
        post.setId(IdGenerator.nextId());
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setAuthorId(user.getId());
        post.setAuthorName(user.getName());
        post.setCreatedAt(DateTimeUtil.now());
        post.setUpdatedAt(DateTimeUtil.now());
        boardRepository.save(post);
        return post;
    }

    public BoardPost update(User user, String id, String title, String content) {
        validate(title, content);
        BoardPost post = findById(id);
        ensureAuthor(post, user.getId());
        post.setTitle(title.trim());
        post.setContent(content.trim());
        post.setUpdatedAt(DateTimeUtil.now());
        boardRepository.save(post);
        return post;
    }

    public void delete(User user, String id) {
        BoardPost post = findById(id);
        ensureAuthor(post, user.getId());
        boardRepository.delete(id);
    }

    private void validate(String title, String content) {
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("제목과 내용을 모두 입력하세요.");
        }
    }

    private void ensureAuthor(BoardPost post, String userId) {
        if (!post.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("본인 게시글만 수정 또는 삭제할 수 있습니다.");
        }
    }
}
