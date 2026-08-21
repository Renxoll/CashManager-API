package pe.smartcash.cash.advisor.domain.services;

import pe.smartcash.cash.advisor.domain.model.queries.AskFinancialAdvisorQuery;

/**
 * Contrato de orquestación RAG del contexto: vive en domain, la implementación ({@code
 * AskFinancialAdvisorUseCaseImpl}) vive en application. No es un {@code CommandService}
 * porque no muta ni persiste nada propio (solo lee vía {@link FinancialContextProvider} y
 * llama a {@link AdvisorChatClient}), así que se nombra como lo que es: un caso de uso de
 * consulta, no una escritura.
 */
public interface AskFinancialAdvisorUseCase {

  AdvisorReply handle(AskFinancialAdvisorQuery query);
}
