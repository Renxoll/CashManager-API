package pe.smartcash.cash.profile.application.internal.queryservices;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfile;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfileRepository;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;
import pe.smartcash.cash.profile.domain.services.UserProfileDetail;
import pe.smartcash.cash.profile.domain.services.UserProfileQueryService;

@Service
@Transactional(readOnly = true)
class UserProfileQueryServiceImpl implements UserProfileQueryService {

  private final UserProfileRepository userProfileRepository;

  UserProfileQueryServiceImpl(UserProfileRepository userProfileRepository) {
    this.userProfileRepository = userProfileRepository;
  }

  @Override
  public Optional<UserProfileDetail> handle(FindUserProfileByIdQuery query) {
    return userProfileRepository.findById(query.userId()).map(this::toDetail);
  }

  @Override
  public boolean exists(UUID userId) {
    return userProfileRepository.findById(UserId.of(userId)).isPresent();
  }

  private UserProfileDetail toDetail(UserProfile profile) {
    return new UserProfileDetail(profile.userId(), profile.displayName(), profile.fcmToken(), profile.createdAt(), profile.updatedAt());
  }
}
