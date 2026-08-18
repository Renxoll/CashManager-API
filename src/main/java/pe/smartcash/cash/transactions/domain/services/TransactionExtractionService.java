package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;

/**
 * Puerto hacia el proveedor de LLM. La implementación por defecto habla el dialecto de
 * OpenAI Chat Completions (compatible también con Gemini en modo OpenAI-compat), pero el
 * resto de la app solo depende de esta interfaz.
 */
public interface TransactionExtractionService {

  /** @throws TransactionExtractionFailedException si el proveedor falla o no devuelve JSON válido tras reintento. */
  ExtractionResult extract(String rawText);
}
