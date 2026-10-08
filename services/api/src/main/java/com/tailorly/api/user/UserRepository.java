package com.tailorly.api.user;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
  @Override
  @EntityGraph(attributePaths = "roles")
  Optional<User> findById(@NonNull UUID id);

  @EntityGraph(attributePaths = "roles")
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
