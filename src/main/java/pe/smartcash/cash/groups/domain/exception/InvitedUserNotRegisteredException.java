package pe.smartcash.cash.groups.domain.exception;

/** El email invitado no corresponde a ninguna cuenta de SmartCash -- v1 solo invita a
 * usuarios ya registrados, ver decisión de alcance en el plan de "Grupos". */
public class InvitedUserNotRegisteredException extends RuntimeException {

  public InvitedUserNotRegisteredException(String email) {
    super("No existe ninguna cuenta de SmartCash con el email: " + email);
  }
}
