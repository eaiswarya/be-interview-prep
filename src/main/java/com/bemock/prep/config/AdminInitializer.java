package com.bemock.prep.config;

import com.bemock.prep.model.Role;
import com.bemock.prep.model.User;
import com.bemock.prep.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Registration only ever creates USERs, so the first ADMIN is created at startup
 * from ADMIN_EMAIL / ADMIN_PASSWORD when both are set and the account doesn't exist yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminProperties.email()) || !StringUtils.hasText(adminProperties.password())) {
            return;
        }
        String email = adminProperties.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Created bootstrap admin {}", email);
    }
}
