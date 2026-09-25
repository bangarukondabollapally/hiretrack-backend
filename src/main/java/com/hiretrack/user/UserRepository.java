package com.hiretrack.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository — TASK-005.
 * Spring Data JPA repository for the User aggregate root.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by AuthService to check for duplicate email and to load user at login. */
    Optional<User> findByEmail(String email);

    /** Convenience method used in AuthService.register for duplicate check. */
    boolean existsByEmail(String email);
}
