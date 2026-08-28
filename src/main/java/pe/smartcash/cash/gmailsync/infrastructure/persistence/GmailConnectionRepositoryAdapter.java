package pe.smartcash.cash.gmailsync.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnectionRepository;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;
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

  @Override
  public void save(GmailConnection connection) {
    jpaRepository.save(mapper.toJpaEntity(connection));
  }

  @Override
  public Optional<GmailConnection> findById(GmailConnectionId id) {
    return jpaRepository.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  public List<GmailConnection> findAllByUserId(UserId userId) {
    return jpaRepository.findAllByUserId(userId.value()).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<GmailConnection> findByUserIdAndEmail(UserId userId, String email) {
    return jpaRepository.findByUserIdAndEmail(userId.value(), email).map(mapper::toDomain);
  }

  @Override
  public void delete(GmailConnectionId id) {
    jpaRepository.deleteById(id.value());
  }

  @Override
  public List<GmailConnection> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }
}
