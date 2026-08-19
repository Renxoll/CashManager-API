package pe.smartcash.cash.profile.infrastructure.persistence.jpa.repositories;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.profile.infrastructure.persistence.UserProfileJpaEntity;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileJpaEntity, UUID> {

  Optional<UserProfileJpaEntity> findByInboxAddress(String inboxAddress);
}
