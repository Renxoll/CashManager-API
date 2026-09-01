package pe.smartcash.cash.groups.domain.services;

import java.util.List;
import pe.smartcash.cash.groups.domain.model.queries.FindGroupDetailQuery;
import pe.smartcash.cash.groups.domain.model.queries.FindMyGroupsQuery;
import pe.smartcash.cash.groups.domain.model.queries.FindMyPendingInvitesQuery;

/** Contrato de lectura del bounded context: vive en domain, la implementación vive en application. */
public interface GroupQueryService {

  List<GroupSummary> handle(FindMyGroupsQuery query);

  /** Lanza {@code GroupNotFoundException} si el grupo no existe o el requester no es
   * miembro ACEPTADO -- ver el criterio "404, no 403" del resto de la app. */
  GroupDetail handle(FindGroupDetailQuery query);

  List<PendingInviteDetail> handle(FindMyPendingInvitesQuery query);
}
