package com.enjoytrip.controller;

import com.enjoytrip.model.User;
import com.enjoytrip.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController extends BaseController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> body, HttpSession session) {
        User user = userService.register(body.get("id"), body.get("name"), body.get("password"));
        setLoginUser(session, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> body, HttpSession session) {
        User user = userService.login(body.get("id"), body.get("password"));
        setLoginUser(session, user);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        clearSession(session);
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(currentUser(session));
    }

    @PutMapping("/me")
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User updated = userService.updateProfile(
                currentUser(session).getId(),
                body.get("name"),
                body.get("currentPassword"),
                body.get("newPassword")
        );
        setLoginUser(session, updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, String>> deleteAccount(@RequestBody Map<String, String> body,
                                                              HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.delete(currentUser(session).getId(), body.get("currentPassword"));
        clearSession(session);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }

    @PostMapping("/find-password")
    public ResponseEntity<Map<String, String>> findPassword(@RequestBody Map<String, String> body) {
        String message = userService.findPasswordMessage(body.get("id"), body.get("name"));
        return ResponseEntity.ok(Map.of("message", message));
    }
}
