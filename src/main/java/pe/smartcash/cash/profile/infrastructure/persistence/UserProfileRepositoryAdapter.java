package pe.smartcash.cash.profile.infrastructure.persistence;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfile;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfileRepository;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;
import pe.smartcash.cash.profile.infrastructure.persistence.jpa.repositories.UserProfileJpaRepository;

@Repository
class UserProfileRepositoryAdapter implements UserProfileRepository {

  private final UserProfileJpaRepository jpaRepository;

  UserProfileRepositoryAdapter(UserProfileJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<UserProfile> findById(UserId userId) {
    return jpaRepository.findById(userId.value()).map(UserProfileEntityMapper::toDomain);
  }

  @Override
  public Optional<UserProfile> findByInboxAddress(String inboxAddress) {
    return jpaRepository.findByInboxAddress(inboxAddress).map(UserProfileEntityMapper::toDomain);
  }

  @Override
  public void save(UserProfile userProfile) {
    jpaRepository.save(UserProfileEntityMapper.toJpaEntity(userProfile));
  }
}
