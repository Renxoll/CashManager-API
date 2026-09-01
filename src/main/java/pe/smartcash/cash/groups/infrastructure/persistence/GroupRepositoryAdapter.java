package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.aggregates.Group;
import pe.smartcash.cash.groups.domain.model.aggregates.GroupRepository;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.GroupJpaRepository;

@Repository
class GroupRepositoryAdapter implements GroupRepository {

  private final GroupJpaRepository jpaRepository;
  private final GroupEntityMapper mapper;

  GroupRepositoryAdapter(GroupJpaRepository jpaRepository, GroupEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(Group group) {
    jpaRepository.save(mapper.toJpaEntity(group));
  }

  @Override
  public Optional<Group> findById(GroupId id) {
    return jpaRepository.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public List<Group> findAllByMemberUserId(UserId userId) {
    return jpaRepository.findAllByMemberUserId(userId.value()).stream().map(mapper::toDomain).toList();
  }
}
