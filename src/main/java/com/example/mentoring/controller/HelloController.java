package com.example.mentoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/")
    public String hello() {
        return "Mentoring Spring Boot 백엔드가 정상적으로 동작합니다.";
    }
}
