package pe.smartcash.cash.groups.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public interface GroupRepository {

  void save(Group group);

  Optional<Group> findById(GroupId id);

  /** Grupos donde el usuario tiene una membresía ACCEPTED -- ver GroupMembership. */
  List<Group> findAllByMemberUserId(UserId userId);
}
