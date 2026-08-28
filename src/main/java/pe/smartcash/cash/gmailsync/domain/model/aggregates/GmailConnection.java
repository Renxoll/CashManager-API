package pe.smartcash.cash.gmailsync.domain.model.aggregates;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context GmailSync: la conexión OAuth de un usuario con UNA de
 * sus bandejas de Gmail (un usuario puede tener varias -- ver {@code
 * GmailConnectionRepository.findAllByUserId}), alternativa a reenviar correos a mano (ver el
 * flujo de SendGrid Inbound Parse en Transactions). Los tokens viajan como {@code String}
 * opaco acá adentro a propósito -- el cifrado en reposo es un detalle de infraestructura (ver
 * infrastructure.crypto.TokenCipher), no una regla de negocio del dominio.
 */
public final class GmailConnection {

  /** Margen de seguridad antes del vencimiento real: evita usar un token a punto de expirar
   * en medio de una llamada a la API de Gmail. */
  private static final Duration EXPIRY_SAFETY_MARGIN = Duration.ofSeconds(60);

  private final GmailConnectionId id;
  private final UserId userId;
  /** Nullable: no se supo el email real hasta que se agregó el scope userinfo -- filas
   * conectadas antes de eso quedan sin verificar hasta que el usuario reconecte una vez. */
  private final String email;
  private String accessToken;
  private String refreshToken;
  private Instant accessTokenExpiresAt;
  private Instant lastSyncedAt;
  private final Instant connectedAt;
  private Instant updatedAt;

  private GmailConnection(
      GmailConnectionId id,
      UserId userId,
      String email,
      String accessToken,
      String refreshToken,
      Instant accessTokenExpiresAt,
      Instant lastSyncedAt,
      Instant connectedAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    this.email = email;
    this.accessToken = requireNonBlank(accessToken, "accessToken");
    this.refreshToken = requireNonBlank(refreshToken, "refreshToken");
    this.accessTokenExpiresAt = Objects.requireNonNull(accessTokenExpiresAt, "accessTokenExpiresAt");
    this.lastSyncedAt = lastSyncedAt;
    this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public static GmailConnection connect(
      GmailConnectionId id, UserId userId, String email, String accessToken, String refreshToken, Instant accessTokenExpiresAt, Instant now) {
    return new GmailConnection(id, userId, email, accessToken, refreshToken, accessTokenExpiresAt, null, now, now);
  }

  public static GmailConnection rehydrate(
      GmailConnectionId id,
      UserId userId,
      String email,
      String accessToken,
      String refreshToken,
      Instant accessTokenExpiresAt,
      Instant lastSyncedAt,
      Instant connectedAt,
      Instant updatedAt) {
    return new GmailConnection(id, userId, email, accessToken, refreshToken, accessTokenExpiresAt, lastSyncedAt, connectedAt, updatedAt);
  }

  /**
   * Google no siempre reemite el refresh token en cada refresh (solo la primera vez o si
   * el usuario revoca y reconecta) -- por eso {@code newRefreshToken} es nullable: si no
   * viene, se conserva el actual.
   */
  public void refreshAccessToken(String newAccessToken, String newRefreshToken, Instant newExpiresAt, Instant now) {
    this.accessToken = requireNonBlank(newAccessToken, "newAccessToken");
    if (newRefreshToken != null && !newRefreshToken.isBlank()) {
      this.refreshToken = newRefreshToken;
    }
    this.accessTokenExpiresAt = Objects.requireNonNull(newExpiresAt, "newExpiresAt");
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public void recordSync(Instant now) {
    this.lastSyncedAt = Objects.requireNonNull(now, "now");
    this.updatedAt = now;
  }

  public boolean needsRefresh(Instant now) {
    return !now.plus(EXPIRY_SAFETY_MARGIN).isBefore(accessTokenExpiresAt);
  }

  private static String requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " no puede estar vacío");
    }
    return value;
  }

  public GmailConnectionId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public String email() {
    return email;
  }

  public String accessToken() {
    return accessToken;
  }

  public String refreshToken() {
    return refreshToken;
  }

  public Instant accessTokenExpiresAt() {
    return accessTokenExpiresAt;
  }

  /** {@code null} hasta el primer poll exitoso: úsese como límite inferior de la búsqueda
   * en Gmail (correos ya vistos no se reprocesan). */
  public Instant lastSyncedAt() {
    return lastSyncedAt;
  }

  public Instant connectedAt() {
    return connectedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
