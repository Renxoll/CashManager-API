package pe.smartcash.cash.groups.infrastructure.persistence;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembership;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

@Component
class GroupMembershipEntityMapper {

  GroupMembershipJpaEntity toJpaEntity(GroupMembership membership) {
    return GroupMembershipJpaEntity.builder()
        .id(membership.id().value())
        .groupId(membership.groupId().value())
        .userId(membership.userId().value())
        .status(membership.status().name())
        .invitedAt(membership.invitedAt())
        .respondedAt(membership.respondedAt())
        .build();
  }

  GroupMembership toDomain(GroupMembershipJpaEntity entity) {
    return GroupMembership.rehydrate(
        MembershipId.of(entity.getId()),
        GroupId.of(entity.getGroupId()),
        UserId.of(entity.getUserId()),
        MembershipStatus.valueOf(entity.getStatus()),
        entity.getInvitedAt(),
        entity.getRespondedAt());
  }
}
