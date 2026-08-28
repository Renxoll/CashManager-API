package pe.smartcash.cash.gmailsync.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.smartcash.cash.gmailsync.infrastructure.persistence.GmailConnectionJpaEntity;

public interface GmailConnectionJpaRepository extends JpaRepository<GmailConnectionJpaEntity, UUID> {

  List<GmailConnectionJpaEntity> findAllByUserId(UUID userId);

  Optional<GmailConnectionJpaEntity> findByUserIdAndEmail(UUID userId, String email);
}
