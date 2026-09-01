package pe.smartcash.cash.groups.infrastructure.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.groups.domain.model.aggregates.Settlement;
import pe.smartcash.cash.groups.domain.model.aggregates.SettlementRepository;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories.SettlementJpaRepository;

@Repository
class SettlementRepositoryAdapter implements SettlementRepository {

  private final SettlementJpaRepository jpaRepository;
  private final SettlementEntityMapper mapper;

  SettlementRepositoryAdapter(SettlementJpaRepository jpaRepository, SettlementEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(Settlement settlement) {
    jpaRepository.save(mapper.toJpaEntity(settlement));
  }

  @Override
  public List<Settlement> findAllByGroupId(GroupId groupId) {
    return jpaRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId.value()).stream().map(mapper::toDomain).toList();
  }
}
