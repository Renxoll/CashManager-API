package pe.smartcash.cash.iam.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

@Repository
class CredentialsRepositoryAdapter implements CredentialsRepository {

  private final CredentialsJpaRepository jpaRepository;

  CredentialsRepositoryAdapter(CredentialsJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<Credentials> findByEmail(Email email) {
    return jpaRepository.findByEmail(email.value()).map(CredentialsEntityMapper::toDomain);
  }

  @Override
  public Optional<Credentials> findById(UserId id) {
    return jpaRepository.findById(id.value()).map(CredentialsEntityMapper::toDomain);
  }

  @Override
  public void save(Credentials credentials) {
    jpaRepository.save(CredentialsEntityMapper.toJpaEntity(credentials));
  }
}
