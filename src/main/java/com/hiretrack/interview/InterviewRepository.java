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

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a WHERE a.user.id = :userId AND i.interviewDate >= :start AND i.outcome = 'PENDING' ORDER BY i.interviewDate ASC")
    List<Interview> findUpcomingInterviews(@Param("userId") Long userId, @Param("start") LocalDateTime start, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a WHERE a.user.id = :userId AND i.interviewDate >= :start AND i.interviewDate <= :end AND i.outcome = 'PENDING' ORDER BY i.interviewDate ASC")
    List<Interview> findUpcomingInterviews(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a WHERE a.user.id = :userId " +
           "AND (:applicationId IS NULL OR a.id = :applicationId) " +
           "AND (:outcome IS NULL OR i.outcome = :outcome) " +
           "ORDER BY i.interviewDate ASC")
    List<Interview> findAllUserInterviews(@Param("userId") Long userId,
                                         @Param("applicationId") Long applicationId,
                                         @Param("outcome") InterviewOutcome outcome);

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a WHERE a.user.id = :userId " +
           "AND (:applicationId IS NULL OR a.id = :applicationId) " +
           "AND (:outcome IS NULL OR i.outcome = :outcome) " +
           "AND i.interviewDate >= :now " +
           "ORDER BY i.interviewDate ASC")
    List<Interview> findUpcomingUserInterviews(@Param("userId") Long userId,
                                             @Param("applicationId") Long applicationId,
                                             @Param("outcome") InterviewOutcome outcome,
                                             @Param("now") LocalDateTime now);

    @Query("SELECT i FROM Interview i JOIN FETCH i.application a WHERE a.user.id = :userId " +
           "AND (:applicationId IS NULL OR a.id = :applicationId) " +
           "AND (:outcome IS NULL OR i.outcome = :outcome) " +
           "AND i.interviewDate < :now " +
           "ORDER BY i.interviewDate DESC")
    List<Interview> findPastUserInterviews(@Param("userId") Long userId,
                                         @Param("applicationId") Long applicationId,
                                         @Param("outcome") InterviewOutcome outcome,
                                         @Param("now") LocalDateTime now);
}

