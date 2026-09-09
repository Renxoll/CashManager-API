package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupDeletionRequest;
import pe.smartcash.cash.groups.domain.model.valueobjects.DeletionRequestStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupDeletionRequestId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

@Component
class GroupDeletionRequestEntityMapper {

  GroupDeletionRequestJpaEntity toRequestJpaEntity(GroupDeletionRequest request) {
    return GroupDeletionRequestJpaEntity.builder()
        .id(request.id().value())
        .groupId(request.groupId().value())
        .requestedBy(request.requestedBy().value())
        .status(request.status().name())
        .requestedAt(request.requestedAt())
        .resolvedAt(request.resolvedAt())
        .build();
  }

  List<GroupDeletionApprovalJpaEntity> toApprovalJpaEntities(GroupDeletionRequest request) {
    return request.approvedBy().stream()
        .map(
            userId ->
                GroupDeletionApprovalJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .requestId(request.id().value())
                    .userId(userId.value())
                    .approvedAt(request.requestedAt())
                    .build())
        .toList();
  }

  GroupDeletionRequest toDomain(
      GroupDeletionRequestJpaEntity entity, List<GroupDeletionApprovalJpaEntity> approvalEntities) {
    Set<UserId> approvals =
        approvalEntities.stream().map(a -> UserId.of(a.getUserId())).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    return GroupDeletionRequest.rehydrate(
        GroupDeletionRequestId.of(entity.getId()),
        GroupId.of(entity.getGroupId()),
        UserId.of(entity.getRequestedBy()),
        entity.getRequestedAt(),
        approvals,
        DeletionRequestStatus.valueOf(entity.getStatus()),
        entity.getResolvedAt());
  }
}
