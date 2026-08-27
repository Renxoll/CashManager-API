package pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.transactions.infrastructure.persistence.CategoryJpaEntity;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

  Optional<CategoryJpaEntity> findByCode(String code);

  List<CategoryJpaEntity> findAllByOrderById();
}
