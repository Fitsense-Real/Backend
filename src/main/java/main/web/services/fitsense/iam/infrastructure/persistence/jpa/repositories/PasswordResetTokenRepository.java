package main.web.services.fitsense.iam.infrastructure.persistence.jpa.repositories;

import jakarta.persistence.LockModeType;
import main.web.services.fitsense.iam.domain.model.aggregates.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    long countByUserIdAndCreatedAtAfter(Long userId, OffsetDateTime desde);

    /** Con bloqueo: dos confirmaciones simultaneas del mismo token no pueden usarlo dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :hash")
    Optional<PasswordResetToken> bloquearPorHash(String hash);

    /** Al cambiar la contrasena, cualquier otro token pendiente del usuario deja de valer. */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :ahora WHERE t.userId = :userId AND t.usedAt IS NULL")
    int invalidarPendientes(Long userId, OffsetDateTime ahora);
}
