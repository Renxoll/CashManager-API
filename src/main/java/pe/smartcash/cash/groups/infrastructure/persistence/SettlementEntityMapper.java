package pe.smartcash.cash.groups.infrastructure.persistence;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.aggregates.Settlement;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.Money;
import pe.smartcash.cash.groups.domain.model.valueobjects.SettlementId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

@Component
class SettlementEntityMapper {

  SettlementJpaEntity toJpaEntity(Settlement settlement) {
    return SettlementJpaEntity.builder()
        .id(settlement.id().value())
        .groupId(settlement.groupId().value())
        .fromUserId(settlement.fromUserId().value())
        .toUserId(settlement.toUserId().value())
        .amount(settlement.amount().amount())
        .currency(settlement.amount().currency())
        .createdAt(settlement.createdAt())
        .build();
  }

  Settlement toDomain(SettlementJpaEntity entity) {
    return Settlement.rehydrate(
        SettlementId.of(entity.getId()),
        GroupId.of(entity.getGroupId()),
        UserId.of(entity.getFromUserId()),
        UserId.of(entity.getToUserId()),
        new Money(entity.getAmount(), entity.getCurrency()),
        entity.getCreatedAt());
  }
}
