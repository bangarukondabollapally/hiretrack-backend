package com.hiretrack.user;

import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.user.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileDto getProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return profileRepository.findByUserId(user.getId())
                .map(this::mapToDto)
                .orElseGet(() -> ProfileDto.builder()
                        .name("")
                        .avatarPreset(null)
                        .resumeText("")
                        .targetRole("")
                        .yearsOfExperience(null)
                        .experienceSummary("")
                        .build());
    }

    @Transactional
    public ProfileDto updateProfile(ProfileDto dto, String userEmail) {
        return updateProfile(dto, dto.getYearsOfExperience() != null, userEmail);
    }

    @Transactional
    public ProfileDto updateProfile(ProfileDto dto, boolean hasYearsOfExperience, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getAvatarPreset() != null && !dto.getAvatarPreset().trim().isEmpty()) {
            String preset = dto.getAvatarPreset().trim();
            if (!preset.matches("^preset-(1[0-2]|[1-9])$")) {
                throw new IllegalArgumentException("Invalid avatar preset key: " + preset);
            }
        }

        if (dto.getAvatarDataUrl() != null && !dto.getAvatarDataUrl().trim().isEmpty()) {
            String dataUrl = dto.getAvatarDataUrl().trim();
            if (!dataUrl.startsWith("data:image/jpeg;base64,") &&
                !dataUrl.startsWith("data:image/png;base64,") &&
                !dataUrl.startsWith("data:image/webp;base64,")) {
                throw new IllegalArgumentException("avatarDataUrl must be a JPEG, PNG, or WebP base64 data URL");
            }
            if (dataUrl.length() > 270000) {
                throw new IllegalArgumentException("avatarDataUrl exceeds 200 KB size limit");
            }
        }

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        if (dto.getName() != null) {
            profile.setName(dto.getName());
        }
        if (dto.getAvatarPreset() != null) {
            profile.setAvatarPreset(dto.getAvatarPreset().trim().isEmpty() ? null : dto.getAvatarPreset().trim());
        }
        if (dto.getAvatarDataUrl() != null) {
            profile.setAvatarDataUrl(dto.getAvatarDataUrl().trim().isEmpty() ? null : dto.getAvatarDataUrl().trim());
        }
        if (dto.getResumeText() != null) {
            profile.setResumeText(dto.getResumeText());
        }
        if (dto.getTargetRole() != null) {
            profile.setTargetRole(dto.getTargetRole());
        }
        if (hasYearsOfExperience) {
            profile.setYearsOfExperience(dto.getYearsOfExperience());
        }
        if (dto.getExperienceSummary() != null) {
            profile.setExperienceSummary(dto.getExperienceSummary());
        }

        Profile saved = profileRepository.save(profile);
        return mapToDto(saved);
    }

    private ProfileDto mapToDto(Profile profile) {
        return ProfileDto.builder()
                .name(profile.getName() != null ? profile.getName() : "")
                .avatarPreset(profile.getAvatarPreset())
                .avatarDataUrl(profile.getAvatarDataUrl())
                .resumeText(profile.getResumeText() != null ? profile.getResumeText() : "")
                .targetRole(profile.getTargetRole() != null ? profile.getTargetRole() : "")
                .yearsOfExperience(profile.getYearsOfExperience())
                .experienceSummary(profile.getExperienceSummary() != null ? profile.getExperienceSummary() : "")
                .build();
    }
}
