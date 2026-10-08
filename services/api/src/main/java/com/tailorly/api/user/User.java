package com.tailorly.api.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.owner.Owner;
import com.tailorly.api.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Builder.Default
  @ToString.Exclude
  @ManyToMany
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"),
      uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
  )
  private Set<Role> roles = new HashSet<>();

  @ToString.Exclude
  @OneToOne(mappedBy = "user")
  private Owner owner;
}
