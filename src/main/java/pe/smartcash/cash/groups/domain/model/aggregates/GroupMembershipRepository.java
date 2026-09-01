package pe.smartcash.cash.groups.domain.model.aggregates;

import java.util.List;
import java.util.Optional;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public interface GroupMembershipRepository {

  void save(GroupMembership membership);

  Optional<GroupMembership> findById(MembershipId id);

  Optional<GroupMembership> findByGroupIdAndUserId(GroupId groupId, UserId userId);

  List<GroupMembership> findAllByGroupId(GroupId groupId);

  List<GroupMembership> findAllByUserIdAndStatus(UserId userId, MembershipStatus status);
}
