package pe.smartcash.cash.transactions.domain.model.valueobjects;

/** De dónde salió el par (comercio -> categoría): del LLM real o del atajo de cache. */
public enum ExtractionSource {
  LLM,
  CACHE
}
