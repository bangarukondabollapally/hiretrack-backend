package com.hiretrack.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByAppliedDateDescIdDesc(Long userId);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT a.status as status, COUNT(a) as count FROM Application a WHERE a.user.id = :userId GROUP BY a.status")
    List<Object[]> countByStatusGrouped(@Param("userId") Long userId);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.followUpDate IS NOT NULL AND a.followUpDate <= :today AND a.status NOT IN ('OFFER', 'REJECTED', 'WITHDRAWN') ORDER BY a.followUpDate ASC")
    List<Application> findFollowUpsDue(@Param("userId") Long userId, @Param("today") LocalDate today);
}
