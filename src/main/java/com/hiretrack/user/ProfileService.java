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
                .map(profile -> ProfileDto.builder()
                        .resumeText(profile.getResumeText() != null ? profile.getResumeText() : "")
                        .targetRole(profile.getTargetRole() != null ? profile.getTargetRole() : "")
                        .build())
                .orElseGet(() -> ProfileDto.builder().resumeText("").targetRole("").build());
    }

    @Transactional
    public ProfileDto updateProfile(ProfileDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        profile.setResumeText(dto.getResumeText());
        // Only overwrite targetRole if the caller actually provided it (non-null)
        if (dto.getTargetRole() != null) {
            profile.setTargetRole(dto.getTargetRole());
        }
        Profile saved = profileRepository.save(profile);

        return ProfileDto.builder()
                .resumeText(saved.getResumeText() != null ? saved.getResumeText() : "")
                .targetRole(saved.getTargetRole() != null ? saved.getTargetRole() : "")
                .build();
    }
}
