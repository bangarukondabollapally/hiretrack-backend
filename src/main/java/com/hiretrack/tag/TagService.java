package com.hiretrack.tag;

import com.hiretrack.application.Application;
import com.hiretrack.application.ApplicationRepository;
import com.hiretrack.common.exception.ForbiddenException;
import com.hiretrack.common.exception.ResourceNotFoundException;
import com.hiretrack.tag.dto.TagRequestDto;
import com.hiretrack.tag.dto.TagResponseDto;
import com.hiretrack.user.User;
import com.hiretrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TagResponseDto> getUserTags(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return tagRepository.findByUserIdOrderByNameAsc(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagResponseDto createTag(TagRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Reuse tag if already exists for this user
        Tag tag = tagRepository.findByNameAndUserId(request.getName().trim(), user.getId())
                .orElseGet(() -> tagRepository.save(
                        Tag.builder()
                                .name(request.getName().trim())
                                .user(user)
                                .build()
                ));

        return mapToDto(tag);
    }

    @Transactional
    public void assignTagToApplication(Long applicationId, Long tagId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to modify this application");
        }

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (!tag.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to use this tag");
        }

        application.getTags().add(tag);
        applicationRepository.save(application);
    }

    @Transactional
    public void removeTagFromApplication(Long applicationId, Long tagId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (!application.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not have permission to modify this application");
        }

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        application.getTags().remove(tag);
        applicationRepository.save(application);
    }

    public TagResponseDto mapToDto(Tag tag) {
        return TagResponseDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .build();
    }
}
