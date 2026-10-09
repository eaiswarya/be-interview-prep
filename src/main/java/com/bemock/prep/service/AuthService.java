package com.bemock.prep.service;

import com.bemock.prep.dto.LoginRequest;
import com.bemock.prep.dto.LoginResponse;
import com.bemock.prep.dto.RegisterRequest;
import com.bemock.prep.dto.UserResponse;
import com.bemock.prep.exception.ConflictException;
import com.bemock.prep.exception.UnauthorizedException;
import com.bemock.prep.model.Role;
import com.bemock.prep.model.User;
import com.bemock.prep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /** Public registration always creates a USER. The unique index on email decides concurrent duplicates. */
    public UserResponse register(RegisterRequest request) {
        User user = new User();
        user.setEmail(normalize(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        try {
            return UserResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ConflictException("Email is already registered");
        }
    }

    /** Same message for unknown email and wrong password, so the response doesn't reveal which emails exist. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.email()))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return tokenService.issue(user);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
