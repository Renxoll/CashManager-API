package pe.smartcash.cash.iam.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.iam.infrastructure.persistence.PasswordResetTokenJpaEntity;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

  Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

  void deleteByUserId(UUID userId);
}
