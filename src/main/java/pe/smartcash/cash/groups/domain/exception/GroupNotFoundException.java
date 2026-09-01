package pe.smartcash.cash.groups.domain.exception;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

/**
 * El grupo no existe, o existe pero el usuario no es miembro ACEPTADO -- ambos casos
 * responden 404, nunca 403: un cliente no debe poder distinguir "no existe" de "no eres
 * miembro" probando ids ajenos.
 */
public class GroupNotFoundException extends RuntimeException {

  public GroupNotFoundException(GroupId groupId) {
    super("Grupo no encontrado: " + groupId.value());
  }
}
