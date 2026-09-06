package pe.smartcash.cash.iam.infrastructure.persistence;

import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetToken;
import pe.smartcash.cash.iam.domain.model.valueobjects.PasswordResetTokenId;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

final class PasswordResetTokenEntityMapper {

  private PasswordResetTokenEntityMapper() {}

  static PasswordResetTokenJpaEntity toJpaEntity(PasswordResetToken token) {
    return PasswordResetTokenJpaEntity.builder()
        .id(token.id().value())
        .userId(token.userId().value())
        .tokenHash(token.tokenHash())
        .createdAt(token.createdAt())
        .expiresAt(token.expiresAt())
        .redeemedAt(token.redeemedAt())
        .build();
  }

  static PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
    return PasswordResetToken.rehydrate(
        PasswordResetTokenId.of(entity.getId()),
        UserId.of(entity.getUserId()),
        entity.getTokenHash(),
        entity.getCreatedAt(),
        entity.getExpiresAt(),
        entity.getRedeemedAt());
  }
}
