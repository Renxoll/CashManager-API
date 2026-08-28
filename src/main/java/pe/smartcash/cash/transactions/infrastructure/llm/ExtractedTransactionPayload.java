package pe.smartcash.cash.transactions.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** DTO de cable: espejo exacto del JSON Schema exigido al LLM. No es un tipo de dominio. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ExtractedTransactionPayload(BigDecimal monto, String moneda, String comercio, String categoria, String tipo) {}
