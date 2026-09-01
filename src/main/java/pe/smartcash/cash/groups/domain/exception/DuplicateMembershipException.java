package pe.smartcash.cash.groups.domain.exception;

/** Ya hay una invitación pendiente o aceptada para ese usuario en ese grupo -- ver el índice
 * único parcial en V13__create_groups_schema.sql, este chequeo es el mecanismo primario. */
public class DuplicateMembershipException extends RuntimeException {

  public DuplicateMembershipException() {
    super("Ese usuario ya tiene una invitación pendiente o ya es miembro de este grupo");
  }
}
