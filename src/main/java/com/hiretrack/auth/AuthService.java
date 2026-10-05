package com.hiretrack.auth;

import com.hiretrack.auth.dto.LoginRequestDto;
import com.hiretrack.auth.dto.LoginResponseDto;
import com.hiretrack.auth.dto.RegisterRequestDto;
import com.hiretrack.auth.dto.RegisterResponseDto;
import com.hiretrack.common.exception.EmailAlreadyExistsException;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hiretrack.user.Role;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${hiretrack.admin.email:}")
    private String adminEmailConfig;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String userEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        Role assignedRole = Role.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String requestedRole = request.getRole().trim().toUpperCase();
            if ("ADMIN".equals(requestedRole) || "PLACEMENT_CELL".equals(requestedRole)) {
                assignedRole = Role.ADMIN;
            } else if ("USER".equals(requestedRole) || "STUDENT".equals(requestedRole)) {
                assignedRole = Role.USER;
            } else {
                assignedRole = Role.USER;
            }
        }

        if (adminEmailConfig != null && !adminEmailConfig.isBlank()) {
            List<String> adminEmails = Arrays.stream(adminEmailConfig.split(","))
                    .map(e -> e.trim().toLowerCase())
                    .toList();
            if (adminEmails.contains(userEmail)) {
                assignedRole = Role.ADMIN;
            }
        }

        User user = User.builder()
                .email(userEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);

        return RegisterResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .role(savedUser.getRole() != null ? savedUser.getRole().name() : "USER")
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Role role = user.getRole() != null ? user.getRole() : Role.USER;

        String token = jwtService.generateToken(user.getEmail(), user.getId(), role);

        return LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .role(role.name())
                .build();
    }
}
