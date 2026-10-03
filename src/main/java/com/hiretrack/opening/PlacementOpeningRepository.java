package com.hiretrack.opening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementOpeningRepository extends JpaRepository<PlacementOpening, Long> {
    List<PlacementOpening> findByStatusOrderByCreatedAtDesc(OpeningStatus status);
    List<PlacementOpening> findAllByOrderByCreatedAtDesc();
}
