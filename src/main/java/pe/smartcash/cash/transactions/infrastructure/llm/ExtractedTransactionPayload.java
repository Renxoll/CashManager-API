package pe.smartcash.cash.transactions.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;

/** DTO de cable: espejo exacto del JSON Schema exigido al LLM. No es un tipo de dominio. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ExtractedTransactionPayload(BigDecimal monto, String moneda, String comercio, String categoria) {

  /**
   * Convierte la respuesta del LLM en un resultado de dominio. Siempre EXPENSE a propósito:
   * los ingresos se registran a mano (ver {@code TransactionCommandServiceImpl.handle(RecordManualIncomeCommand)}),
   * porque al usuario casi nunca le llegan por correo los depósitos propios.
   *
   * <p>Un JSON bien formado pero con valores que el dominio rechaza (moneda "S/" en vez de
   * "PEN", comercio vacío, monto nulo) es un fallo de extracción igual que un JSON roto: se
   * traduce a {@link TransactionExtractionFailedException} para que el respaldo (Groq) tenga
   * su oportunidad y, si también falla, la transacción quede FAILED con el motivo -- en vez de
   * que una {@code IllegalArgumentException} escape del worker async y la deje PENDING.
   */
  ExtractionResult toResult() {
    try {
      // Un monto con más de 2 decimales (p. ej. 10.000) se redondea en vez de rechazarse.
      BigDecimal amount = monto != null && monto.scale() > 2 ? monto.setScale(2, RoundingMode.HALF_UP) : monto;
      return new ExtractionResult(
          new Money(amount, moneda == null ? null : moneda.trim().toUpperCase()),
          new Merchant(comercio),
          CategoryCode.fromCode(categoria),
          TransactionType.EXPENSE);
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new TransactionExtractionFailedException("El LLM devolvió valores que no son una transacción válida: " + invalid.getMessage(), invalid);
    }
  }
}
