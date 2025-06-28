package com.example.mentoring.entity;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // mentor 또는 mentee

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String bio; // 소개글

    @Column(length = 1000)
    private String profileImageUrl; // 프로필 이미지 URL

    @Column(length = 500)
    private String techStack; // 멘토만: 기술 스택(쉼표 구분)

    // ...getter, setter, 생성자 생략...

    public User() {}

    public User(String email, String password, String role, String name) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    public User updateProfile(String name, String bio, String profileImageUrl, String techStack) {
        if (name != null) this.name = name;
        if (bio != null) this.bio = bio;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (techStack != null) this.techStack = techStack;
        return this;
    }
}
