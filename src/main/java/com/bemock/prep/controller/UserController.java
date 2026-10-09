package com.bemock.prep.controller;

import com.bemock.prep.dto.UserResponse;
import com.bemock.prep.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** The token subject is the user id, so a user can only ever see their own profile here. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.get(Long.valueOf(jwt.getSubject()));
    }

    /** ADMIN only, enforced in {@code SecurityConfig}. */
    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }
}
