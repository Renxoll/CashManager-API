package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.smartcash.cash.groups.infrastructure.persistence.SettlementJpaEntity;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, UUID> {

  List<SettlementJpaEntity> findAllByGroupIdOrderByCreatedAtDesc(UUID groupId);

  /** Parte del borrado en cascada del grupo por consenso. */
  @Modifying
  @Query("delete from SettlementJpaEntity s where s.groupId = :groupId")
  void deleteByGroupId(UUID groupId);
}
