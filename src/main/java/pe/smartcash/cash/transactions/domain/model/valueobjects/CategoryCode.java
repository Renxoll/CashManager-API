package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Locale;

/**
 * Catálogo cerrado de categorías: es la misma enumeración que se le exige al LLM en el
 * Structured Output (ver ExtractionPrompts), así que es el dueño de la palabra final sobre
 * qué categorías existen. La fila de catálogo (nombre visible, ícono) es un detalle de
 * infraestructura resuelto vía el puerto {@link pe.smartcash.cash.transactions.domain.services.CategoryCatalog}.
 */
public enum CategoryCode {
  COMIDA,
  TRANSPORTE,
  ENTRETENIMIENTO,
  SALUD,
  COMPRAS,
  SERVICIOS,
  EDUCACION,
  OTROS;

  /** Cualquier código desconocido o no parseable cae a OTROS (regla 6 del prompt de extracción). */
  public static CategoryCode fromCode(String raw) {
    if (raw == null) {
      return OTROS;
    }
    try {
      return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return OTROS;
    }
  }
}
