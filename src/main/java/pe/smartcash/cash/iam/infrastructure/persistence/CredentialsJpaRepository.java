package pe.smartcash.cash.iam.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CredentialsJpaRepository extends JpaRepository<CredentialsJpaEntity, UUID> {

  Optional<CredentialsJpaEntity> findByEmail(String email);
}
