package pe.smartcash.cash.advisor.application.internal.queryservices;

import java.time.Clock;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.advisor.domain.model.queries.AskFinancialAdvisorQuery;
import pe.smartcash.cash.advisor.domain.services.AdvisorChatClient;
import pe.smartcash.cash.advisor.domain.services.AdvisorReply;
import pe.smartcash.cash.advisor.domain.services.AskFinancialAdvisorUseCase;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;
import pe.smartcash.cash.advisor.domain.services.FinancialContextProvider;

/**
 * Orquestación RAG: 1) trae el contexto financiero real del usuario (Analytics, vía ACL),
 * 2) se lo pasa al puerto de LLM junto con la pregunta -- el prompt en sí (cómo se combinan
 * datos + pregunta en texto) es prompt engineering y vive en infrastructure ({@code
 * AdvisorPrompts}), no acá. Sin {@code @Transactional}: no hay escritura ni persistencia
 * propia, solo lecturas y una llamada HTTP saliente.
 */
@Service
class AskFinancialAdvisorUseCaseImpl implements AskFinancialAdvisorUseCase {

  private final FinancialContextProvider financialContextProvider;
  private final AdvisorChatClient advisorChatClient;
  private final Clock clock;

  AskFinancialAdvisorUseCaseImpl(FinancialContextProvider financialContextProvider, AdvisorChatClient advisorChatClient, Clock clock) {
    this.financialContextProvider = financialContextProvider;
    this.advisorChatClient = advisorChatClient;
    this.clock = clock;
  }

  @Override
  public AdvisorReply handle(AskFinancialAdvisorQuery query) {
    FinancialContext context = financialContextProvider.currentMonthContext(query.userId());
    String reply = advisorChatClient.reply(context, query.message());
    return new AdvisorReply(reply, clock.instant());
  }
}
