package pe.smartcash.cash.transactions.infrastructure.llm;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.services.ExtractionResult;
import pe.smartcash.cash.transactions.domain.services.TransactionExtractionService;

/**
 * Único bean que implementa el puerto {@link TransactionExtractionService}: compone dos
 * proveedores de LLM, {@link OpenAiTransactionExtractionAdapter} como primario y {@link
 * GrokTransactionExtractionAdapter} como respaldo -- mismo patrón que {@code
 * advisor.infrastructure.llm.FallbackAdvisorChatClient}. Si el primario falla (cuota diaria
 * de Gemini excedida, timeout, JSON inválido tras reintento) se reintenta con el respaldo
 * antes de marcar la transacción FAILED; solo si ambos fallan se propaga la excepción (con
 * el fallo original adjunto como suprimido, para no perder esa causa en Sentry).
 */
@Component
class FallbackTransactionExtractionService implements TransactionExtractionService {

  private final OpenAiTransactionExtractionAdapter primary;
  private final GrokTransactionExtractionAdapter fallback;

  FallbackTransactionExtractionService(OpenAiTransactionExtractionAdapter primary, GrokTransactionExtractionAdapter fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public ExtractionResult extract(String rawText) {
    try {
      return primary.extract(rawText);
    } catch (TransactionExtractionFailedException primaryFailure) {
      try {
        return fallback.extract(rawText);
      } catch (TransactionExtractionFailedException fallbackFailure) {
        fallbackFailure.addSuppressed(primaryFailure);
        throw fallbackFailure;
      }
    }
  }
}
