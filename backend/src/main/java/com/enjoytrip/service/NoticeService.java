package com.enjoytrip.service;

import com.enjoytrip.model.Notice;
import com.enjoytrip.model.User;
import com.enjoytrip.repository.NoticeRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.IdGenerator;

import java.util.List;

public class NoticeService {
    private final NoticeRepository noticeRepository;

    public NoticeService(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    public List<Notice> findAll() {
        return noticeRepository.findAll();
    }

    public Notice findById(String id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
    }

    public Notice create(User user, String title, String content) {
        ensureAdmin(user);
        validate(title, content);
        Notice notice = new Notice();
        notice.setId(IdGenerator.nextId());
        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setAuthorId(user.getId());
        notice.setAuthorName(user.getName());
        notice.setCreatedAt(DateTimeUtil.now());
        notice.setUpdatedAt(DateTimeUtil.now());
        noticeRepository.save(notice);
        return notice;
    }

    public Notice update(User user, String id, String title, String content) {
        ensureAdmin(user);
        validate(title, content);
        Notice notice = findById(id);
        notice.setTitle(title.trim());
        notice.setContent(content.trim());
        notice.setUpdatedAt(DateTimeUtil.now());
        noticeRepository.save(notice);
        return notice;
    }

    public void delete(User user, String id) {
        ensureAdmin(user);
        noticeRepository.delete(id);
    }

    private void validate(String title, String content) {
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("제목과 내용을 모두 입력하세요.");
        }
    }

    private void ensureAdmin(User user) {
        if (!user.isAdmin()) {
            throw new IllegalArgumentException("관리자만 공지사항을 작성·수정·삭제할 수 있습니다.");
        }
    }
}
