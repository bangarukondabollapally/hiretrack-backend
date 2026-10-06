package com.hiretrack.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(length = 255)
    private String targetRole;

    @Column
    private Integer yearsOfExperience;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String experienceSummary;

    @Column(length = 50)
    private String avatarPreset;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String avatarDataUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
