package pe.smartcash.cash.transactions.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

  Optional<CategoryJpaEntity> findByCode(String code);
}
