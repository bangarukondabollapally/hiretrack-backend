package com.hiretrack.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByNameAsc(Long userId);

    Optional<Tag> findByIdAndUserId(Long id, Long userId);

    Optional<Tag> findByNameAndUserId(String name, Long userId);

    boolean existsByNameAndUserId(String name, Long userId);
}
