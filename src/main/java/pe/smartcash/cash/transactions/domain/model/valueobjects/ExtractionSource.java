package pe.smartcash.cash.transactions.domain.model.valueobjects;

/** De dónde salió la transacción: del LLM real, del atajo de cache, o cargada a mano por el
 * usuario (ver Transaction.recordManualIncome -- los ingresos no se detectan por correo). */
public enum ExtractionSource {
  LLM,
  CACHE,
  MANUAL
}
