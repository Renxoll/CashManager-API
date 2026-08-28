package pe.smartcash.cash.transactions.domain.model.valueobjects;

import java.util.Locale;

/**
 * Dirección del dinero: eje ortogonal a {@link CategoryCode} (una cosa es "cuánto salió y en
 * qué se fue", otra "si entró o salió"). A diferencia de {@code CategoryCode} (cuyos valores
 * SON las palabras en español que exige el prompt), acá el enum se queda en inglés porque se
 * usa mucho más allá del LLM (columna de BD, respuesta REST, filtros de analytics) -- {@link
 * #fromCode} es la traducción explícita en el borde desde "GASTO"/"INGRESO" (lo que pide
 * ExtractionPrompts), no una coincidencia de nombre como en CategoryCode.
 */
public enum TransactionType {
  EXPENSE,
  INCOME;

  /** Desconocido/null/"GASTO" cae a EXPENSE (mismo criterio conservador que CategoryCode.fromCode -> OTROS); "INGRESO" -> INCOME. */
  public static TransactionType fromCode(String raw) {
    if (raw == null) {
      return EXPENSE;
    }
    return "INGRESO".equals(raw.trim().toUpperCase(Locale.ROOT)) ? INCOME : EXPENSE;
  }
}
