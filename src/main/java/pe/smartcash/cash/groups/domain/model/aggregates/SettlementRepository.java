package pe.smartcash.cash.groups.domain.model.aggregates;

import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

public interface SettlementRepository {

  void save(Settlement settlement);

  /** Más recientes primero. */
  List<Settlement> findAllByGroupId(GroupId groupId);
}
