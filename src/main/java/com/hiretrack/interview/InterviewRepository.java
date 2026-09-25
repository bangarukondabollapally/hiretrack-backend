package com.hiretrack.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByApplicationIdOrderByInterviewDateAsc(Long applicationId);

    Optional<Interview> findByIdAndApplicationId(Long id, Long applicationId);

    @Query("SELECT i FROM Interview i JOIN i.application a WHERE a.user.id = :userId AND i.interviewDate >= :start AND i.interviewDate <= :end AND i.outcome = 'PENDING' ORDER BY i.interviewDate ASC")
    List<Interview> findUpcomingInterviews(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
