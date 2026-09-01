package pe.smartcash.cash.groups.domain.exception;

import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;

/** La invitación no existe, o existe pero no es del usuario autenticado -- mismo 404 en
 * ambos casos, mismo criterio que {@code GroupNotFoundException}. */
public class MembershipNotFoundException extends RuntimeException {

  public MembershipNotFoundException(MembershipId membershipId) {
    super("Invitación no encontrada: " + membershipId.value());
  }
}
