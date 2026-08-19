package pe.smartcash.cash.iam.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.iam.infrastructure.persistence.CredentialsJpaEntity;

public interface CredentialsJpaRepository extends JpaRepository<CredentialsJpaEntity, UUID> {

  Optional<CredentialsJpaEntity> findByEmail(String email);
}
