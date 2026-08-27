package pe.smartcash.cash.gmailsync.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.infrastructure.persistence.jpa.repositories.GmailConnectionJpaRepository;

@Repository
class GmailConnectionRepositoryAdapter implements GmailConnectionRepository {

  private final GmailConnectionJpaRepository jpaRepository;
  private final GmailConnectionEntityMapper mapper;

  GmailConnectionRepositoryAdapter(GmailConnectionJpaRepository jpaRepository, GmailConnectionEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  /** {@code user_id} es UNIQUE en la tabla: si ya existía una conexión para este usuario
   * (reconectar), se actualiza la misma fila en vez de violar la constraint. */
  @Override
  public void save(GmailConnection connection) {
    UUID existingId =
        jpaRepository.findByUserId(connection.userId().value()).map(GmailConnectionJpaEntity::getId).orElse(null);
    jpaRepository.save(mapper.toJpaEntity(connection, existingId));
  }

  @Override
  public Optional<GmailConnection> findByUserId(UserId userId) {
    return jpaRepository.findByUserId(userId.value()).map(mapper::toDomain);
  }

  @Override
  public List<GmailConnection> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }
}
