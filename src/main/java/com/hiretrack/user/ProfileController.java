package com.hiretrack.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiretrack.user.dto.ProfileDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        ProfileDto profile = profileService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileDto> updateProfile(
            @RequestBody String rawJson,
            @AuthenticationPrincipal UserDetails userDetails
    ) throws Exception {
        JsonNode node = objectMapper.readTree(rawJson);
        ProfileDto dto = objectMapper.treeToValue(node, ProfileDto.class);

        Set<ConstraintViolation<ProfileDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        boolean hasYearsOfExperience = node.has("yearsOfExperience");
        ProfileDto updated = profileService.updateProfile(dto, hasYearsOfExperience, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }
}
