package pe.smartcash.cash.iam.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/**
 * Aggregate root del bounded context IAM. Su única razón de ser es autenticación: quién
 * puede entrar (email) y con qué contraseña (ya hasheada — nunca guarda ni conoce la
 * contraseña en texto plano). No sabe nada de nombres, avatares ni suscripciones: eso es
 * responsabilidad de otros contextos (Profile, Subscription).
 */
public final class Credentials {

  private final UserId id;
  private final Email email;
  private HashedPassword hashedPassword;
  private final Instant createdAt;

  private Credentials(UserId id, Email email, HashedPassword hashedPassword, Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.email = Objects.requireNonNull(email, "email");
    this.hashedPassword = Objects.requireNonNull(hashedPassword, "hashedPassword");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public static Credentials register(UserId id, Email email, HashedPassword hashedPassword, Instant now) {
    return new Credentials(id, email, hashedPassword, now);
  }

  public static Credentials rehydrate(UserId id, Email email, HashedPassword hashedPassword, Instant createdAt) {
    return new Credentials(id, email, hashedPassword, createdAt);
  }

  public UserId id() {
    return id;
  }

  public Email email() {
    return email;
  }

  public HashedPassword hashedPassword() {
    return hashedPassword;
  }

  public Instant createdAt() {
    return createdAt;
  }
}
