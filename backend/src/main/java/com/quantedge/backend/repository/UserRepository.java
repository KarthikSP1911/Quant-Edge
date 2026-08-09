package com.quantedge.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    // Emails aren't normalized to a single case at the DB level, so login (typed fresh by a
    // human, often differently-cased than at signup) must match case-insensitively.
    Optional<User> findByEmailIgnoreCase(String email);
}
