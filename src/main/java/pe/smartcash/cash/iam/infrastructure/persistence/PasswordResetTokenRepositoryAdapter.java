package pe.smartcash.cash.iam.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetToken;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetTokenRepository;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.infrastructure.persistence.jpa.repositories.PasswordResetTokenJpaRepository;

@Repository
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

  private final PasswordResetTokenJpaRepository jpaRepository;

  PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public void save(PasswordResetToken token) {
    jpaRepository.save(PasswordResetTokenEntityMapper.toJpaEntity(token));
  }

  @Override
  public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
    return jpaRepository.findByTokenHash(tokenHash).map(PasswordResetTokenEntityMapper::toDomain);
  }

  @Override
  @Transactional
  public void deleteAllByUserId(UserId userId) {
    // Derived delete de Spring Data: necesita transacción propia -- el caso de uso que la
    // llama ya corre @Transactional, pero se anota igual para que funcione si alguna vez se
    // invoca suelta.
    jpaRepository.deleteByUserId(userId.value());
  }
}
