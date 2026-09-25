package com.hiretrack.interview;

import com.hiretrack.application.Application;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String round;

    @Column(nullable = false)
    private LocalDateTime interviewDate;

    @Column(length = 50)
    private String interviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewOutcome outcome;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (outcome == null) {
            outcome = InterviewOutcome.PENDING;
        }
    }
}
