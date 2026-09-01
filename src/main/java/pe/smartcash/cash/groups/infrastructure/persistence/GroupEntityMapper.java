package pe.smartcash.cash.groups.infrastructure.persistence;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.aggregates.Group;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

@Component
class GroupEntityMapper {

  GroupJpaEntity toJpaEntity(Group group) {
    return GroupJpaEntity.builder()
        .id(group.id().value())
        .name(group.name())
        .ownerId(group.ownerId().value())
        .createdAt(group.createdAt())
        .build();
  }

  Group toDomain(GroupJpaEntity entity) {
    return Group.rehydrate(GroupId.of(entity.getId()), entity.getName(), UserId.of(entity.getOwnerId()), entity.getCreatedAt());
  }
}
