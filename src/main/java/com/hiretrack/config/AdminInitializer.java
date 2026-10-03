package com.hiretrack.config;

import com.hiretrack.user.Role;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hiretrack.admin.email:}")
    private String adminEmail;

    @Value("${hiretrack.admin.password:}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeAdminUser() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        String trimmedEmail = adminEmail.trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(trimmedEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getRole() == Role.ADMIN) {
                // Admin already exists and is configured
                return;
            } else {
                // User exists as USER role — DO NOT silently promote an existing student account!
                log.warn("Configured admin email matches existing student account with USER role. Promotion skipped for security.");
                return;
            }
        }

        User adminUser = User.builder()
                .email(trimmedEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(adminUser);
        log.info("Admin account provisioned successfully via configuration.");
    }
}
