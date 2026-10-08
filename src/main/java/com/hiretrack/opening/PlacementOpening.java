package com.hiretrack.opening;

import com.hiretrack.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "placement_openings",
    indexes = {
        @Index(name = "idx_opening_status", columnList = "status"),
        @Index(name = "idx_opening_deadline", columnList = "deadline")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String jobRole;

    @Column(length = 50)
    private String jobType;

    @Column(length = 100)
    private String location;

    @Column(length = 50)
    private String workMode;

    @Column(length = 100)
    private String packageDetails;

    @Column(length = 500)
    private String eligibility;

    @Column(length = 50)
    private String degree;

    @Column(name = "degree_types", length = 255)
    private String degreeTypes;

    @Column(length = 255)
    private String eligibleBranches;

    private Integer graduationYearStart;

    private Integer graduationYearEnd;

    @Column(length = 50)
    private String yearOfStudy;

    private Double minCgpa;

    private Integer maxBacklogs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    private LocalDate deadline;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 1000)
    private String applicationLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OpeningStatus status = OpeningStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = OpeningStatus.OPEN;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
