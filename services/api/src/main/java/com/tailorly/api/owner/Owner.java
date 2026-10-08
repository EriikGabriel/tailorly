package com.tailorly.api.owner;

import java.time.Instant;
import java.util.UUID;

import com.tailorly.api.session.AnonymousSession;
import com.tailorly.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@Entity
@Check(
    name = "chk_owners_kind_user",
    constraints = "kind = 'KIND_ACCOUNT' "
        + "OR (kind = 'KIND_ANONYMOUS' AND user_id IS NULL)"
)
@Table(name = "owners")
public class Owner {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "kind", nullable = false)
  @Enumerated(EnumType.STRING)
  private OwnerKindEnum kind;

  @ToString.Exclude
  @OneToOne
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @Column(name = "active_snapshot_id")
  private UUID activeSnapshotId;

  @Column(name = "revision", nullable = false)
  private int revision;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @ToString.Exclude
  @OneToOne(mappedBy = "owner")
  private AnonymousSession anonymousSession;

}
