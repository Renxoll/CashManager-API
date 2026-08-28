package pe.smartcash.cash.advisor.infrastructure.llm;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.advisor.domain.exception.AdvisorUnavailableException;
import pe.smartcash.cash.advisor.domain.services.AdvisorChatClient;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;

/**
 * Único bean que implementa el puerto {@link AdvisorChatClient}: compone dos proveedores de
 * LLM, {@link GeminiFinancialAdvisorAdapter} como primario y {@link GrokFinancialAdvisorAdapter}
 * como respaldo. Si Gemini falla (cuota diaria excedida, timeout, respuesta vacía) se reintenta
 * con Grok antes de devolverle un 503 al usuario; solo si ambos fallan se propaga la excepción
 * (con el fallo original de Gemini adjunto como suprimido, para no perder esa causa en Sentry).
 */
@Component
class FallbackAdvisorChatClient implements AdvisorChatClient {

  private final GeminiFinancialAdvisorAdapter primary;
  private final GrokFinancialAdvisorAdapter fallback;

  FallbackAdvisorChatClient(GeminiFinancialAdvisorAdapter primary, GrokFinancialAdvisorAdapter fallback) {
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public String reply(FinancialContext context, String question) {
    try {
      return primary.reply(context, question);
    } catch (AdvisorUnavailableException primaryFailure) {
      try {
        return fallback.reply(context, question);
      } catch (AdvisorUnavailableException fallbackFailure) {
        fallbackFailure.addSuppressed(primaryFailure);
        throw fallbackFailure;
      }
    }
  }
}
