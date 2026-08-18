package pe.smartcash.cash.profile.infrastructure.persistence;

import pe.smartcash.cash.profile.domain.model.aggregates.UserProfile;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

final class UserProfileEntityMapper {

  private UserProfileEntityMapper() {}

  static UserProfileJpaEntity toJpaEntity(UserProfile profile) {
    return UserProfileJpaEntity.builder()
        .id(profile.userId().value())
        .displayName(profile.displayName())
        .fcmToken(profile.fcmToken())
        .createdAt(profile.createdAt())
        .updatedAt(profile.updatedAt())
        .build();
  }

  static UserProfile toDomain(UserProfileJpaEntity entity) {
    return UserProfile.rehydrate(
        UserId.of(entity.getId()), entity.getDisplayName(), entity.getFcmToken(), entity.getCreatedAt(), entity.getUpdatedAt());
  }
}
