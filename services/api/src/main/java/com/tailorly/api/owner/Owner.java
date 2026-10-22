package com.tailorly.api.owner;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.tailorly.api.base.document.BaseDocument;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.review.ReviewDecision;
import com.tailorly.api.base.snapshot.GtSnapshot;
import com.tailorly.api.objects.StoredObject;
import com.tailorly.api.session.AnonymousSession;
import com.tailorly.api.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
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
    constraints = "(kind = 'KIND_ACCOUNT' AND user_id IS NOT NULL) "
        + "OR (kind = 'KIND_ANONYMOUS' AND user_id IS NULL)"
)
/**
 * Identidade proprietária de dados: uma conta autenticada ({@code KIND_ACCOUNT}) ou uma sessão
 * anônima ({@code KIND_ANONYMOUS}). O check constraint garante que contas sempre tenham
 * {@code user} e sessões anônimas nunca tenham. {@code activeSnapshotId} aponta para a versão
 * do Currículo Base aprovada como Ground Truth ativo (fluxo ainda planejado); {@code revision}
 * é incrementado atomicamente a cada mudança relevante do owner.
 */
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

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "active_snapshot_id", insertable = false, updatable = false)
  private GtSnapshot activeSnapshot;

  @Column(name = "revision", nullable = false)
  private int revision;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @ToString.Exclude
  @OneToOne(mappedBy = "owner")
  private AnonymousSession anonymousSession;

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
  private Set<StoredObject> storedObjects = new HashSet<>();

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
  private Set<BaseDocument> baseDocuments = new HashSet<>();

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
  private Set<BaseVersion> baseVersions = new HashSet<>();

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
  private Set<GtSnapshot> snapshots = new HashSet<>();

  @Builder.Default
  @ToString.Exclude
  @OneToMany(mappedBy = "actorOwner", fetch = FetchType.LAZY)
  private Set<ReviewDecision> reviewDecisions = new HashSet<>();

}
