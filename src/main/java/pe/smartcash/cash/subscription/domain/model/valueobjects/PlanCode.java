package pe.smartcash.cash.subscription.domain.model.valueobjects;

import java.time.Duration;
import java.util.Locale;

/** Catálogo cerrado de planes de la plataforma. */
public enum PlanCode {
  FREE(null),
  PREMIUM(Duration.ofDays(30));

  private final Duration term;

  PlanCode(Duration term) {
    this.term = term;
  }

  /** Vigencia del plan; {@code null} = no expira (FREE). */
  public Duration term() {
    return term;
  }

  public static PlanCode fromCode(String raw) {
    return valueOf(raw.trim().toUpperCase(Locale.ROOT));
  }
}
