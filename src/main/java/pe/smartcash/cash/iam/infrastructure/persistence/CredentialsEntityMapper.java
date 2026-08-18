package pe.smartcash.cash.iam.infrastructure.persistence;

import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

final class CredentialsEntityMapper {

  private CredentialsEntityMapper() {}

  static CredentialsJpaEntity toJpaEntity(Credentials credentials) {
    return CredentialsJpaEntity.builder()
        .id(credentials.id().value())
        .email(credentials.email().value())
        .hashedPassword(credentials.hashedPassword().value())
        .createdAt(credentials.createdAt())
        .build();
  }

  static Credentials toDomain(CredentialsJpaEntity entity) {
    return Credentials.rehydrate(
        UserId.of(entity.getId()), new Email(entity.getEmail()), new HashedPassword(entity.getHashedPassword()), entity.getCreatedAt());
  }
}
