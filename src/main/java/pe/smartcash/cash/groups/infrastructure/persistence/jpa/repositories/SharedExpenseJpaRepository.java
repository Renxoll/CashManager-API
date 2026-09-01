package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.groups.infrastructure.persistence.SharedExpenseJpaEntity;

public interface SharedExpenseJpaRepository extends JpaRepository<SharedExpenseJpaEntity, UUID> {

  List<SharedExpenseJpaEntity> findAllByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
