package pe.smartcash.cash.groups.domain.exception;

/** Un miembro del grupo que no pagó el gasto ni es el owner intentó corregirlo -- 403: sí es
 * miembro (ve el grupo), pero no tiene permiso sobre este gasto puntual. */
public class ExpenseEditForbiddenException extends RuntimeException {

  public ExpenseEditForbiddenException() {
    super("Solo quien pagó el gasto o el owner del grupo puede corregirlo");
  }
}
