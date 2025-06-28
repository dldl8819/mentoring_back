package com.example.mentoring.service;

import com.example.mentoring.entity.User;
import com.example.mentoring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}
