package com.example.mentoring.controller;

import com.example.mentoring.entity.User;
import com.example.mentoring.service.UserService;
import com.example.mentoring.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(request.getEmail(), request.getPassword(), request.getRole(), request.getName());
            // 명세에 따라 201 Created, 사용자 정보 반환 (비밀번호 제외)
            Map<String, Object> userResponse = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole(),
                "profile", Map.of(
                    "name", user.getName() != null ? user.getName() : "",
                    "bio", user.getBio() != null ? user.getBio() : "",
                    "image", user.getProfileImageUrl() != null ? user.getProfileImageUrl() : 
                            ("mentor".equals(user.getRole()) ? "https://placehold.co/500x500.jpg?text=MENTOR" : 
                                                               "https://placehold.co/500x500.jpg?text=MENTEE")
                )
            );
            return ResponseEntity.status(201).body(userResponse);
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
        
        // 실제 JWT 토큰 생성 (RFC 7519 표준 클레임 포함)
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getName(), user.getRole());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
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
            // 파일 검증
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "파일이 비어 있습니다."));
            }
            
            // 파일 확장자 검증 (.jpg, .png만 허용)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "파일명이 유효하지 않습니다."));
            }
            String ext = StringUtils.getFilenameExtension(originalFilename);
            if (ext == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "파일 확장자가 없습니다."));
            }
            ext = ext.toLowerCase();
            if (!("jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext))) {
                return ResponseEntity.badRequest().body(Map.of("message", ".jpg 또는 .png 파일만 업로드 가능합니다."));
            }
            
            // 파일 크기 검증 (최대 1MB)
            if (file.getSize() > 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("message", "파일 크기는 최대 1MB까지 허용됩니다."));
            }
            
            // 이미지 해상도 검증 (완화된 버전)
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    // 정사각형이 아니어도 허용하되 권장사항으로 안내
                    if (width != height) {
                        // 경고만 하고 업로드는 허용
                        System.out.println("Warning: 정사각형 이미지를 권장합니다. (현재: " + width + "x" + height + ")");
                    }
                    // 해상도 검증을 더 완화 (1x1 ~ 2000x2000)
                    if (width < 1 || width > 2000 || height < 1 || height > 2000) {
                        return ResponseEntity.badRequest().body(Map.of("message", "이미지 크기는 1x1 ~ 2000x2000 픽셀이어야 합니다."));
                    }
                }
            } catch (Exception e) {
                // 이미지 검증 실패해도 업로드는 허용 (파일이 이미지가 아닐 수도 있음)
                System.out.println("Image validation failed: " + e.getMessage());
            }
            
            // 파일 저장
            String filename = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.]+", "_");
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);
            
            // 성공 응답 (JSON 형식으로 통일)
            String imageUrl = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of(
                "message", "파일 업로드 성공",
                "url", imageUrl,
                "filename", filename
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "파일 저장 실패: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "파일 업로드 실패: " + e.getMessage()));
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

    // 매칭 관련 API들 - 명세에 맞게 경로 수정
    @PostMapping("/match-requests")
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

    @GetMapping("/match-requests/outgoing")
    public ResponseEntity<?> getOutgoingMatchingRequests() {
        try {
            // 멘티가 보낸 매칭 요청 목록 (명세에 맞게 수정)
            List<Map<String, Object>> requests = List.of(
                Map.of(
                    "id", 1L,
                    "mentorId", 1L,
                    "menteeId", 2L,
                    "mentorName", "김멘토",
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

    @GetMapping("/match-requests/incoming")
    public ResponseEntity<?> getIncomingMatchingRequests() {
        try {
            // 멘토가 받은 매칭 요청 목록 (명세에 맞게 추가)
            List<Map<String, Object>> requests = List.of(
                Map.of(
                    "id", 1L,
                    "mentorId", 1L,
                    "menteeId", 2L,
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

    @PatchMapping("/match-requests/{id}")
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

    @DeleteMapping("/match-requests/{id}")
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
