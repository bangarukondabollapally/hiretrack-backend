package com.hiretrack.opening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlacementOpeningRepository extends JpaRepository<PlacementOpening, Long> {
    List<PlacementOpening> findByStatusOrderByCreatedAtDesc(OpeningStatus status);
    List<PlacementOpening> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM PlacementOpening o WHERE o.status = 'OPEN' AND (o.deadline IS NULL OR o.deadline >= :today) ORDER BY o.createdAt DESC")
    List<PlacementOpening> findOpenAndActiveOpenings(@Param("today") LocalDate today);
}
