package pe.smartcash.cash.groups.domain.exception;

/** Alguien involucrado en un gasto o un pago (quien paga, un participante, el destinatario)
 * no es miembro ACEPTADO del grupo. */
public class NotAGroupMemberException extends RuntimeException {

  public NotAGroupMemberException(String message) {
    super(message);
  }
}
