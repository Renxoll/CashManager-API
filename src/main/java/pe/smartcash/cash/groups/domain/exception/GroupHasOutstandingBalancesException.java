package pe.smartcash.cash.groups.domain.exception;

/** No se puede borrar el grupo mientras algún miembro tenga saldo neto distinto de cero --
 * 409: primero hay que saldar todas las deudas (registrar los pagos pendientes). */
public class GroupHasOutstandingBalancesException extends RuntimeException {

  public GroupHasOutstandingBalancesException() {
    super("No se puede borrar el grupo: todavía hay saldos pendientes entre los miembros");
  }
}
