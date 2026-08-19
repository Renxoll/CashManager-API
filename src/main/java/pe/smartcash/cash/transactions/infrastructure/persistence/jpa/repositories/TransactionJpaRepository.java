package pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionStatus;
import pe.smartcash.cash.transactions.infrastructure.persistence.TransactionJpaEntity;

public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {

  List<TransactionJpaEntity> findAllByStatus(TransactionStatus status);
}
