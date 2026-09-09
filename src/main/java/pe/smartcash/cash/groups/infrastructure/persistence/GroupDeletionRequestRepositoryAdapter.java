package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupDeletionRequest;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupDeletionRequestRepository;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.GroupDeletionApprovalJpaRepository;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.GroupDeletionRequestJpaRepository;

/**
 * Agregado de dos tablas ({@code group_deletion_requests} + {@code group_deletion_approvals}),
 * mismo enfoque que {@link SharedExpenseRepositoryAdapter}: el caller ya corre dentro de un
 * {@code @Transactional}, así que ambas escrituras son atómicas.
 */
@Repository
class GroupDeletionRequestRepositoryAdapter implements GroupDeletionRequestRepository {

  private final GroupDeletionRequestJpaRepository requestJpaRepository;
  private final GroupDeletionApprovalJpaRepository approvalJpaRepository;
  private final GroupDeletionRequestEntityMapper mapper;

  GroupDeletionRequestRepositoryAdapter(
      GroupDeletionRequestJpaRepository requestJpaRepository,
      GroupDeletionApprovalJpaRepository approvalJpaRepository,
      GroupDeletionRequestEntityMapper mapper) {
    this.requestJpaRepository = requestJpaRepository;
    this.approvalJpaRepository = approvalJpaRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(GroupDeletionRequest request) {
    requestJpaRepository.save(mapper.toRequestJpaEntity(request));
    approvalJpaRepository.deleteByRequestId(request.id().value());
    approvalJpaRepository.saveAll(mapper.toApprovalJpaEntities(request));
  }

  @Override
  public Optional<GroupDeletionRequest> findPendingByGroupId(GroupId groupId) {
    return requestJpaRepository
        .findByGroupIdAndStatus(groupId.value(), "PENDING")
        .map(entity -> mapper.toDomain(entity, approvalJpaRepository.findAllByRequestId(entity.getId())));
  }

  @Override
  public void deleteAllByGroupId(GroupId groupId) {
    List<UUID> requestIds = requestJpaRepository.findIdsByGroupId(groupId.value());
    if (!requestIds.isEmpty()) {
      approvalJpaRepository.deleteByRequestIdIn(requestIds);
    }
    requestJpaRepository.deleteByGroupId(groupId.value());
  }
}
