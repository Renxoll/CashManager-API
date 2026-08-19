package pe.smartcash.cash.profile.domain.model.aggregates;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Profile: "quién es esta persona dentro de la app"
 * (nombre visible, dispositivo para notificaciones, buzón de ingesta por correo), separado a
 * propósito de IAM (cómo entra) y de Subscription (qué plan paga). El {@link UserId} es el
 * mismo UUID que acuñó IAM al registrar credenciales, pero es un tipo propio de este
 * contexto — no hay dependencia real hacia el módulo iam.
 */
public final class UserProfile {

  private final UserId userId;
  private String displayName;
  private String fcmToken;
  private final String inboxAddress;
  private final Instant createdAt;
  private Instant updatedAt;

  private UserProfile(
      UserId userId, String displayName, String fcmToken, String inboxAddress, Instant createdAt, Instant updatedAt) {
    this.userId = Objects.requireNonNull(userId, "userId");
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
    this.displayName = displayName;
    this.fcmToken = fcmToken;
    if (inboxAddress == null || inboxAddress.isBlank()) {
      throw new IllegalArgumentException("inboxAddress no puede estar vacío");
    }
    this.inboxAddress = inboxAddress;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  /**
   * El dominio de correo (p. ej. {@code inbox.smartcash.pe}) llega como parámetro en vez de
   * estar hardcodeado acá: es un detalle de despliegue (distinto en dev/prod), y el dominio
   * no debe conocer de dónde sale ese valor (config de Spring) para seguir siendo puro.
   */
  public static UserProfile register(UserId userId, String displayName, String inboxDomain, Instant now) {
    return new UserProfile(userId, displayName, null, generateInboxAddress(userId, inboxDomain), now, now);
  }

  public static UserProfile rehydrate(
      UserId userId, String displayName, String fcmToken, String inboxAddress, Instant createdAt, Instant updatedAt) {
    return new UserProfile(userId, displayName, fcmToken, inboxAddress, createdAt, updatedAt);
  }

  /**
   * Determinística a partir del {@link UserId} (no aleatoria): reproducible y fácil de
   * depurar, y un hash de 10 hex en vez del UUID crudo evita que la dirección de correo
   * pública delate o permita enumerar ids internos de usuario.
   */
  private static String generateInboxAddress(UserId userId, String inboxDomain) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] hash = sha256.digest(userId.value().toString().getBytes(StandardCharsets.UTF_8));
      String alias = HexFormat.of().formatHex(hash).substring(0, 10);
      return "alias-" + alias + "@" + inboxDomain;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
    }
  }

  public void updateFcmToken(String fcmToken, Instant now) {
    if (fcmToken == null || fcmToken.isBlank()) {
      throw new IllegalArgumentException("fcmToken no puede estar vacío");
    }
    this.fcmToken = fcmToken;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public void renameTo(String displayName, Instant now) {
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
    this.displayName = displayName;
    this.updatedAt = Objects.requireNonNull(now, "now");
  }

  public UserId userId() {
    return userId;
  }

  public String displayName() {
    return displayName;
  }

  public String fcmToken() {
    return fcmToken;
  }

  public String inboxAddress() {
    return inboxAddress;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
