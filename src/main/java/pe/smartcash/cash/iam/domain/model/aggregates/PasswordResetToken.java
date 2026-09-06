package pe.smartcash.cash.iam.domain.model.aggregates;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.iam.domain.model.valueobjects.PasswordResetTokenId;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/**
 * Aggregate root del flujo "olvidé mi contraseña". Guarda el <em>hash</em> del token (nunca
 * el token crudo, que solo vive en el enlace del correo) y su ventana de validez. Es de un
 * solo uso: {@link #redeem} lo marca consumido y {@link #isRedeemable} deja de aceptarlo.
 */
public final class PasswordResetToken {

  private final PasswordResetTokenId id;
  private final UserId userId;
  private final String tokenHash;
  private final Instant createdAt;
  private final Instant expiresAt;
  private Instant redeemedAt;

  private PasswordResetToken(
      PasswordResetTokenId id, UserId userId, String tokenHash, Instant createdAt, Instant expiresAt, Instant redeemedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    this.redeemedAt = redeemedAt;
  }

  public static PasswordResetToken issue(PasswordResetTokenId id, UserId userId, String tokenHash, Instant now, Duration ttl) {
    return new PasswordResetToken(id, userId, tokenHash, now, now.plus(ttl), null);
  }

  public static PasswordResetToken rehydrate(
      PasswordResetTokenId id, UserId userId, String tokenHash, Instant createdAt, Instant expiresAt, Instant redeemedAt) {
    return new PasswordResetToken(id, userId, tokenHash, createdAt, expiresAt, redeemedAt);
  }

  public boolean isRedeemable(Instant now) {
    return redeemedAt == null && now.isBefore(expiresAt);
  }

  public void redeem(Instant now) {
    if (!isRedeemable(now)) {
      throw new IllegalStateException("El token de restablecimiento no es redimible (ya usado o expirado)");
    }
    this.redeemedAt = Objects.requireNonNull(now, "now");
  }

  public PasswordResetTokenId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public String tokenHash() {
    return tokenHash;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public Instant redeemedAt() {
    return redeemedAt;
  }
}
