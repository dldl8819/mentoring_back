package com.example.mentoring.controller;

import com.example.mentoring.entity.User;
import com.example.mentoring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserService userService;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword(), request.getRole(), request.getName());
            // 명세에 따라 201 Created, user 정보 JSON 반환
            return ResponseEntity.status(201).body(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Internal server error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }
        User user = userOpt.get();
        if (!userService.getPasswordEncoder().matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }
        // 임시로 간단한 토큰 생성 (실제 JWT 대신)
        String token = "simple-token-" + user.getId();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody ProfileRequest request) {
        try {
            User user = userService.updateProfileFields(userId, request.getName(), request.getBio(), request.getProfileImageUrl(), request.getTechStack());
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        try {
            User user = userService.getProfile(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public static class MentorResponse {
        public Long id;
        public String name;
        public String bio;
        public String profileImageUrl;
        public String[] skills;
        public MentorResponse(User u) {
            this.id = u.getId();
            this.name = u.getName();
            this.bio = u.getBio();
            this.profileImageUrl = u.getProfileImageUrl();
            this.skills = u.getTechStack() == null ? new String[]{} : u.getTechStack().split(",");
        }
    }

    @GetMapping("/mentors")
    public ResponseEntity<?> getMentors(@RequestParam(required = false) String techStack,
                                        @RequestParam(required = false) String sortBy) {
        List<User> mentors = userService.findMentors(techStack, sortBy);
        List<MentorResponse> result = mentors.stream().map(MentorResponse::new).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/profile/upload")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("파일이 비어 있습니다.");
            }
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            if (ext == null || !(ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png"))) {
                return ResponseEntity.badRequest().body("jpg, jpeg, png 파일만 업로드 가능합니다.");
            }
            if (file.getSize() > 1024 * 1024) {
                return ResponseEntity.badRequest().body("1MB 이하 파일만 업로드 가능합니다.");
            }
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]+", "_");
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);
            String url = "/uploads/" + filename;
            return ResponseEntity.ok().body(url);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("파일 업로드 실패: " + e.getMessage());
        }
    }

    public static class SignupRequest {
        @NotBlank(message = "이메일은 필수입니다.")
        public String email;
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
        public String password;
        @NotBlank(message = "역할은 필수입니다.")
        public String role;
        @NotBlank(message = "이름은 필수입니다.")
        public String name;
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
        @NotBlank(message = "이메일은 필수입니다.")
        public String email;
        @NotBlank(message = "비밀번호는 필수입니다.")
        public String password;
        // ...getter, setter...
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ProfileRequest {
        private String name;
        private String bio;
        private String profileImageUrl;
        private String techStack;
        // ...getter, setter...
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
        public String getTechStack() { return techStack; }
        public void setTechStack(String techStack) { this.techStack = techStack; }
    }

    // 매칭 관련 API들
    @PostMapping("/matching-requests")
    public ResponseEntity<?> createMatchingRequest(@RequestBody CreateMatchingRequestDto request) {
        try {
            // 임시 구현: 간단한 매칭 요청 생성
            return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "message", "매칭 요청이 생성되었습니다.",
                "mentorId", request.mentorId,
                "requestMessage", request.message
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/matching-requests")
    public ResponseEntity<?> getMatchingRequests() {
        try {
            // 임시 구현: 샘플 매칭 요청 목록 반환
            List<Map<String, Object>> requests = List.of(
                Map.of(
                    "id", 1L,
                    "mentorId", 1L,
                    "menteeId", 2L,
                    "mentorName", "김멘토",
                    "menteeName", "이멘티",
                    "message", "Java 백엔드 개발을 배우고 싶습니다.",
                    "status", "pending",
                    "createdAt", "2024-01-15T10:30:00"
                )
            );
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/matching-requests/{id}")
    public ResponseEntity<?> updateMatchingRequestStatus(@PathVariable Long id, @RequestBody UpdateStatusDto request) {
        try {
            // 임시 구현: 상태 업데이트
            return ResponseEntity.ok(Map.of(
                "id", id,
                "status", request.status,
                "message", "요청 상태가 업데이트되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/matching-requests/{id}")
    public ResponseEntity<?> deleteMatchingRequest(@PathVariable Long id) {
        try {
            // 임시 구현: 매칭 요청 삭제
            return ResponseEntity.ok(Map.of(
                "id", id,
                "message", "매칭 요청이 삭제되었습니다."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 매칭 관련 DTO 클래스들
    public static class CreateMatchingRequestDto {
        public Long mentorId;
        public String message;
    }

    public static class UpdateStatusDto {
        public String status; // PENDING, ACCEPTED, REJECTED
    }
}
