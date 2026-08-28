package pe.smartcash.cash.transactions.infrastructure.persistence;

import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSender;
import pe.smartcash.cash.transactions.domain.model.valueobjects.PendingSenderId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

final class PendingSenderEntityMapper {

  private PendingSenderEntityMapper() {}

  static PendingSenderJpaEntity toJpaEntity(PendingSender pendingSender) {
    return PendingSenderJpaEntity.builder()
        .id(pendingSender.id().value())
        .userId(pendingSender.userId().value())
        .fromAddress(pendingSender.fromAddress())
        .domain(pendingSender.domain())
        .sampleSnippet(pendingSender.sampleSnippet())
        .status(pendingSender.status())
        .occurrenceCount(pendingSender.occurrenceCount())
        .firstSeenAt(pendingSender.firstSeenAt())
        .lastSeenAt(pendingSender.lastSeenAt())
        .decidedAt(pendingSender.decidedAt())
        .build();
  }

  static PendingSender toDomain(PendingSenderJpaEntity entity) {
    return PendingSender.rehydrate(
        PendingSenderId.of(entity.getId()),
        UserId.of(entity.getUserId()),
        entity.getFromAddress(),
        entity.getDomain(),
        entity.getSampleSnippet(),
        entity.getStatus(),
        entity.getOccurrenceCount(),
        entity.getFirstSeenAt(),
        entity.getLastSeenAt(),
        entity.getDecidedAt());
  }
}
