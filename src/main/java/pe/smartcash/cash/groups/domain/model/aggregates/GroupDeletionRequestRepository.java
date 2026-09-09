package pe.smartcash.cash.groups.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

public interface GroupDeletionRequestRepository {

  /** Persiste la solicitud y sus aprobaciones en la misma transacción -- ver el adaptador. */
  void save(GroupDeletionRequest request);

  /** La solicitud PENDING de un grupo, si la hay -- a lo sumo una a la vez (índice parcial). */
  Optional<GroupDeletionRequest> findPendingByGroupId(GroupId groupId);

  /** Borra toda solicitud (y sus aprobaciones) del grupo -- parte del borrado en cascada. */
  void deleteAllByGroupId(GroupId groupId);
}
