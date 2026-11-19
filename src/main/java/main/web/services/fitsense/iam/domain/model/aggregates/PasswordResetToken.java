package main.web.services.fitsense.iam.domain.model.aggregates;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Token de recuperacion de contrasena (V44_2). Solo se guarda el SHA-256 del token: quien lea la tabla no puede
 * usarlo. Un solo uso y caducidad corta.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    protected PasswordResetToken() {
        // JPA
    }

    public PasswordResetToken(Long userId, String tokenHash, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        if (userId == null || tokenHash == null || tokenHash.length() != 64)
            throw new IllegalArgumentException("Token de recuperacion incompleto.");
        if (!expiresAt.isAfter(createdAt))
            throw new IllegalArgumentException("La caducidad debe ser posterior a la creacion.");
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Valido si no se ha usado y no ha caducado en {@code ahora}. */
    public boolean usableAt(OffsetDateTime ahora) {
        return usedAt == null && ahora.isBefore(expiresAt);
    }

    public void markUsed(OffsetDateTime ahora) {
        if (usedAt != null) throw new IllegalStateException("El token ya se uso.");
        this.usedAt = ahora;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
}
