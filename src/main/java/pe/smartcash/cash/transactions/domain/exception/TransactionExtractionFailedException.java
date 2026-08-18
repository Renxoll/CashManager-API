package pe.smartcash.cash.transactions.domain.exception;

/** El puerto de extracción (LLM) no pudo devolver un resultado utilizable. */
public class TransactionExtractionFailedException extends RuntimeException {

  public TransactionExtractionFailedException(String message) {
    super(message);
  }

  public TransactionExtractionFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
