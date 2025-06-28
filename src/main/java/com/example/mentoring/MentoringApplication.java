package com.example.mentoring;

import com.example.mentoring.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MentoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(MentoringApplication.class, args);
    }

    @Bean
    public CommandLineRunner testData(UserService userService) {
        return args -> {
            // 테스트 데이터: 멘토, 멘티 계정 생성
            try {
                userService.registerUser("mentor1@example.com", "mentorpass", "mentor", "멘토1");
                userService.registerUser("mentee1@example.com", "menteepass", "mentee", "멘티1");
            } catch (Exception ignored) {}
        };
    }
}
