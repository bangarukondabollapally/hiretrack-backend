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
                        .name(profile.getName() != null ? profile.getName() : "")
                        .resumeText(profile.getResumeText() != null ? profile.getResumeText() : "")
                        .targetRole(profile.getTargetRole() != null ? profile.getTargetRole() : "")
                        .build())
                .orElseGet(() -> ProfileDto.builder().name("").resumeText("").targetRole("").build());
    }

    @Transactional
    public ProfileDto updateProfile(ProfileDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> Profile.builder().user(user).build());

        // All fields are optional in request body — omitting a field leaves the stored value unchanged
        if (dto.getName() != null) {
            profile.setName(dto.getName());
        }
        if (dto.getResumeText() != null) {
            profile.setResumeText(dto.getResumeText());
        }
        if (dto.getTargetRole() != null) {
            profile.setTargetRole(dto.getTargetRole());
        }
        Profile saved = profileRepository.save(profile);

        return ProfileDto.builder()
                .name(saved.getName() != null ? saved.getName() : "")
                .resumeText(saved.getResumeText() != null ? saved.getResumeText() : "")
                .targetRole(saved.getTargetRole() != null ? saved.getTargetRole() : "")
                .build();
    }
}
