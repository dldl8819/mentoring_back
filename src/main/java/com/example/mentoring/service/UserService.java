package com.example.mentoring.service;

import com.example.mentoring.entity.User;
import com.example.mentoring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User registerUser(String email, String password, String role, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(email, encodedPassword, role, name);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public User updateProfileFields(Long userId, String name, String bio, String profileImageUrl, String techStack) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        user.updateProfile(name, bio, profileImageUrl, techStack);
        return userRepository.save(user);
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    public List<User> findMentors(String techStack, String sortBy) {
        List<User> mentors = userRepository.findAll().stream()
            .filter(u -> "mentor".equals(u.getRole()))
            .filter(u -> techStack == null || (u.getTechStack() != null && u.getTechStack().contains(techStack)))
            .sorted((a, b) -> {
                if ("name".equals(sortBy)) {
                    return a.getName().compareToIgnoreCase(b.getName());
                } else if ("techStack".equals(sortBy)) {
                    return (a.getTechStack() != null ? a.getTechStack() : "").compareToIgnoreCase(b.getTechStack() != null ? b.getTechStack() : "");
                }
                return 0;
            })
            .collect(java.util.stream.Collectors.toList());
        return mentors;
    }
}
