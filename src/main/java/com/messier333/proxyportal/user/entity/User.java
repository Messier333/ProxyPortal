package com.messier333.proxyportal.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column
    private String nickname;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static User createUser(String username, String encodedPassword, Role role) {
        User user = new User();
        user.username = username;
        user.password = encodedPassword;
        user.role = role;
        return user;
    }
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
    public void changePassword(String encodedPassword){
        this.password = encodedPassword;
    }
}
