package pe.smartcash.cash.profile.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context Profile: "quién es esta persona dentro de la app"
 * (nombre visible, dispositivo para notificaciones), separado a propósito de IAM (cómo
 * entra) y de Subscription (qué plan paga). El {@link UserId} es el mismo UUID que acuñó
 * IAM al registrar credenciales, pero es un tipo propio de este contexto — no hay
 * dependencia real hacia el módulo iam.
 */
public final class UserProfile {

  private final UserId userId;
  private String displayName;
  private String fcmToken;
  private final Instant createdAt;
  private Instant updatedAt;

  private UserProfile(UserId userId, String displayName, String fcmToken, Instant createdAt, Instant updatedAt) {
    this.userId = Objects.requireNonNull(userId, "userId");
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
    this.displayName = displayName;
    this.fcmToken = fcmToken;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  public static UserProfile register(UserId userId, String displayName, Instant now) {
    return new UserProfile(userId, displayName, null, now, now);
  }

  public static UserProfile rehydrate(UserId userId, String displayName, String fcmToken, Instant createdAt, Instant updatedAt) {
    return new UserProfile(userId, displayName, fcmToken, createdAt, updatedAt);
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

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }
}
