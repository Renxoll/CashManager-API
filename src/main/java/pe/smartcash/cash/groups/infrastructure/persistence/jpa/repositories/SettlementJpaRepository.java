package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.groups.infrastructure.persistence.SettlementJpaEntity;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, UUID> {

  List<SettlementJpaEntity> findAllByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
