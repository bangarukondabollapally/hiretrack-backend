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

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        if (dto.getName() != null) {
            profile.setName(dto.getName());
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
                .resumeText(profile.getResumeText() != null ? profile.getResumeText() : "")
                .targetRole(profile.getTargetRole() != null ? profile.getTargetRole() : "")
                .yearsOfExperience(profile.getYearsOfExperience())
                .experienceSummary(profile.getExperienceSummary() != null ? profile.getExperienceSummary() : "")
                .build();
    }
}
