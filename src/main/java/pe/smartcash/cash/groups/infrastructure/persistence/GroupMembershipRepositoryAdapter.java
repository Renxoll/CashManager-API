package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembership;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupMembershipRepository;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.GroupMembershipJpaRepository;

@Repository
class GroupMembershipRepositoryAdapter implements GroupMembershipRepository {

  private final GroupMembershipJpaRepository jpaRepository;
  private final GroupMembershipEntityMapper mapper;

  GroupMembershipRepositoryAdapter(GroupMembershipJpaRepository jpaRepository, GroupMembershipEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(GroupMembership membership) {
    jpaRepository.save(mapper.toJpaEntity(membership));
  }

  @Override
  public Optional<GroupMembership> findById(MembershipId id) {
    return jpaRepository.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public Optional<GroupMembership> findByGroupIdAndUserId(GroupId groupId, UserId userId) {
    return jpaRepository.findByGroupIdAndUserId(groupId.value(), userId.value()).map(mapper::toDomain);
  }

  @Override
  public List<GroupMembership> findAllByGroupId(GroupId groupId) {
    return jpaRepository.findAllByGroupId(groupId.value()).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<GroupMembership> findAllByUserIdAndStatus(UserId userId, MembershipStatus status) {
    return jpaRepository.findAllByUserIdAndStatus(userId.value(), status.name()).stream().map(mapper::toDomain).toList();
  }
}
