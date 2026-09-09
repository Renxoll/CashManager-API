package pe.smartcash.cash.groups.domain.exception;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

/** No hay ninguna solicitud de borrado PENDING para el grupo -- 404, no se puede aprobar ni
 * cancelar algo que no existe. */
public class GroupDeletionRequestNotFoundException extends RuntimeException {

  public GroupDeletionRequestNotFoundException(GroupId groupId) {
    super("No hay una solicitud de borrado abierta para el grupo: " + groupId.value());
  }
}
