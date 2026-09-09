package pe.smartcash.cash.groups.domain.exception;

/** Ya hay una solicitud de borrado PENDING para el grupo -- 409, se aprueba o se cancela esa,
 * no se abre otra (ver el índice único parcial en V20). */
public class GroupDeletionAlreadyRequestedException extends RuntimeException {

  public GroupDeletionAlreadyRequestedException() {
    super("Ya hay una solicitud de borrado abierta para este grupo");
  }
}
