package pe.smartcash.cash.advisor.domain.services;

/**
 * Puerto hacia el proveedor de LLM (mismo rol que {@code
 * transactions.domain.services.TransactionExtractionService}): entra/sale con tipos de
 * dominio puros, la implementación ({@code GeminiFinancialAdvisorAdapter}) es la única que
 * sabe de prompts, dialecto Chat Completions o HTTP.
 */
public interface AdvisorChatClient {

  String reply(FinancialContext context, String question);
}
