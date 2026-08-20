package pe.smartcash.cash.advisor.domain.exception;

/** El puerto de LLM ({@code AdvisorChatClient}) no pudo devolver una respuesta utilizable. */
public class AdvisorUnavailableException extends RuntimeException {

  public AdvisorUnavailableException(String message) {
    super(message);
  }

  public AdvisorUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
