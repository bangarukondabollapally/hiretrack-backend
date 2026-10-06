package com.hiretrack.opening;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserTrackedOpeningRepository extends JpaRepository<UserTrackedOpening, Long> {
    Optional<UserTrackedOpening> findByUserIdAndPlacementOpeningId(Long userId, Long openingId);

    boolean existsByUserIdAndPlacementOpeningId(Long userId, Long openingId);

    void deleteByUserIdAndPlacementOpeningId(Long userId, Long openingId);

    @Query("SELECT uto.placementOpening.id FROM UserTrackedOpening uto WHERE uto.user.id = :userId")
    Set<Long> findTrackedOpeningIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT uto.placementOpening FROM UserTrackedOpening uto WHERE uto.user.id = :userId ORDER BY uto.createdAt DESC")
    List<PlacementOpening> findTrackedOpeningsByUserId(@Param("userId") Long userId);
}
