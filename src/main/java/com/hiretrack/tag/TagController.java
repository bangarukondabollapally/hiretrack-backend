package com.hiretrack.tag;

import com.hiretrack.tag.dto.TagRequestDto;
import com.hiretrack.tag.dto.TagResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/api/tags")
    public ResponseEntity<List<TagResponseDto>> getUserTags(@AuthenticationPrincipal UserDetails userDetails) {
        List<TagResponseDto> tags = tagService.getUserTags(userDetails.getUsername());
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/api/tags")
    public ResponseEntity<TagResponseDto> createTag(
            @Valid @RequestBody TagRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TagResponseDto response = tagService.createTag(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/applications/{applicationId}/tags/{tagId}")
    public ResponseEntity<Void> assignTag(
            @PathVariable Long applicationId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        tagService.assignTagToApplication(applicationId, tagId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/applications/{applicationId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(
            @PathVariable Long applicationId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        tagService.removeTagFromApplication(applicationId, tagId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
