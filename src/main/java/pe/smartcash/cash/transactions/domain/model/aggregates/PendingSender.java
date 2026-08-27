package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.time.Instant;
import java.util.Objects;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderStatus;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Un dominio de remitente que le llegó a un usuario pero no está en su lista de confianza (ni
 * la global ni la que él mismo aprobó) -- en vez de descartarlo en silencio como antes, queda
 * acá para que el usuario decida. Aprobar solo habilita los correos FUTUROS de ese dominio
 * (ver {@link UserTrustedSenderRepository}): a propósito no reprocesa el correo que disparó
 * esta fila, para no tener que guardar el texto completo de correos de remitentes todavía sin
 * confirmar.
 */
public final class PendingSender {

  private final PendingSenderId id;
  private final UserId userId;
  private final String fromAddress;
  private final String domain;
  private final String sampleSnippet;
  private final Instant firstSeenAt;

  private PendingSenderStatus status;
  private int occurrenceCount;
  private Instant lastSeenAt;
  private Instant decidedAt;

  private PendingSender(
      PendingSenderId id,
      UserId userId,
      String fromAddress,
      String domain,
      String sampleSnippet,
      PendingSenderStatus status,
      int occurrenceCount,
      Instant firstSeenAt,
      Instant lastSeenAt,
      Instant decidedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.userId = Objects.requireNonNull(userId, "userId");
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalArgumentException("fromAddress no puede estar vacío");
    }
    this.fromAddress = fromAddress;
    if (domain == null || domain.isBlank()) {
      throw new IllegalArgumentException("domain no puede estar vacío");
    }
    this.domain = domain;
    this.sampleSnippet = sampleSnippet;
    this.status = Objects.requireNonNull(status, "status");
    this.occurrenceCount = occurrenceCount;
    this.firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt");
    this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt");
    this.decidedAt = decidedAt;
  }

  /** Primera vez que se ve este dominio para este usuario. */
  public static PendingSender firstSighting(
      PendingSenderId id, UserId userId, String fromAddress, String domain, String sampleSnippet, Instant seenAt) {
    return new PendingSender(id, userId, fromAddress, domain, sampleSnippet, PendingSenderStatus.PENDING, 1, seenAt, seenAt, null);
  }

  /** Reconstrucción desde persistencia: restaura estado sin re-aplicar invariantes de creación. */
  public static PendingSender rehydrate(
      PendingSenderId id,
      UserId userId,
      String fromAddress,
      String domain,
      String sampleSnippet,
      PendingSenderStatus status,
      int occurrenceCount,
      Instant firstSeenAt,
      Instant lastSeenAt,
      Instant decidedAt) {
    return new PendingSender(id, userId, fromAddress, domain, sampleSnippet, status, occurrenceCount, firstSeenAt, lastSeenAt, decidedAt);
  }

  /**
   * Otro correo del mismo dominio, todavía sin decisión del usuario. Si ya se decidió
   * (aprobado o rechazado) no hace nada: un dominio ya rechazado no debe "revivir" solo
   * porque siga llegando correo de ahí -- el usuario ya fue claro.
   */
  public void recordAnotherSighting(Instant seenAt) {
    if (status != PendingSenderStatus.PENDING) {
      return;
    }
    this.occurrenceCount++;
    this.lastSeenAt = Objects.requireNonNull(seenAt, "seenAt");
  }

  public void approve(Instant decidedAt) {
    requireStatus(PendingSenderStatus.PENDING, "aprobar");
    this.status = PendingSenderStatus.APPROVED;
    this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
  }

  public void reject(Instant decidedAt) {
    requireStatus(PendingSenderStatus.PENDING, "rechazar");
    this.status = PendingSenderStatus.REJECTED;
    this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
  }

  private void requireStatus(PendingSenderStatus expected, String action) {
    if (this.status != expected) {
      throw new IllegalStateException(
          "No se puede %s un remitente pendiente en estado %s (se esperaba %s)".formatted(action, status, expected));
    }
  }

  public PendingSenderId id() {
    return id;
  }

  public UserId userId() {
    return userId;
  }

  public String fromAddress() {
    return fromAddress;
  }

  public String domain() {
    return domain;
  }

  public String sampleSnippet() {
    return sampleSnippet;
  }

  public PendingSenderStatus status() {
    return status;
  }

  public int occurrenceCount() {
    return occurrenceCount;
  }

  public Instant firstSeenAt() {
    return firstSeenAt;
  }

  public Instant lastSeenAt() {
    return lastSeenAt;
  }

  public Instant decidedAt() {
    return decidedAt;
  }
}
