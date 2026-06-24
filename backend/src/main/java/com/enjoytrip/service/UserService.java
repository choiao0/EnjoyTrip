package com.enjoytrip.service;

import com.enjoytrip.model.User;
import com.enjoytrip.repository.UserRepository;
import com.enjoytrip.util.DateTimeUtil;
import com.enjoytrip.util.PasswordHasher;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String id, String name, String rawPassword) {
        if (id == null || id.isBlank() || name == null || name.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("아이디, 이름, 비밀번호를 모두 입력하세요.");
        }
        if (userRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = new User();
        user.setId(id.trim());
        user.setName(name.trim());
        user.setPwHash(PasswordHasher.hash(rawPassword));
        user.setCreatedAt(DateTimeUtil.now());
        user.setUpdatedAt(DateTimeUtil.now());
        userRepository.save(user);
        return user;
    }

    public User login(String id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!PasswordHasher.matches(rawPassword, user.getPwHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    public User updateProfile(String userId, String name, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if (!PasswordHasher.matches(currentPassword, user.getPwHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력하세요.");
        }
        user.setName(name.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPwHash(PasswordHasher.hash(newPassword));
        }
        user.setUpdatedAt(DateTimeUtil.now());
        userRepository.save(user);
        return user;
    }

    public void delete(String userId, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if (!PasswordHasher.matches(currentPassword, user.getPwHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        userRepository.delete(userId);
    }

    public void changeRole(String userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        user.setRole(role);
        user.setUpdatedAt(DateTimeUtil.now());
        userRepository.save(user);
    }

    public String findPasswordMessage(String id, String name) {
        if (userRepository.findByIdAndName(id, name).isPresent()) {
            return "비밀번호는 해시로 저장되어 조회할 수 없습니다. 로그인 후 마이페이지에서 새 비밀번호를 설정하세요.";
        }
        throw new IllegalArgumentException("일치하는 회원이 없습니다.");
    }
}
