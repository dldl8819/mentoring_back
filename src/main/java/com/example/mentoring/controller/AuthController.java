package com.example.mentoring.controller;

import com.example.mentoring.entity.User;
import com.example.mentoring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword(), request.getRole(), request.getName());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        User user = userOpt.get();
        if (!userService.getPasswordEncoder().matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        // JWT 토큰 발급 예시 (실제 구현 필요)
        String token = "dummy-jwt-token";
        return ResponseEntity.ok().body("JWT: " + token);
    }

    public static class SignupRequest {
        private String email;
        private String password;
        private String role;
        private String name;
        // ...getter, setter...
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class LoginRequest {
        private String email;
        private String password;
        // ...getter, setter...
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
