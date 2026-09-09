package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.smartcash.cash.groups.infrastructure.persistence.GroupDeletionApprovalJpaEntity;

public interface GroupDeletionApprovalJpaRepository extends JpaRepository<GroupDeletionApprovalJpaEntity, UUID> {

  List<GroupDeletionApprovalJpaEntity> findAllByRequestId(UUID requestId);

  /** Reemplaza las aprobaciones en cada {@code save} de la solicitud -- bulk DELETE JPQL a
   * propósito, mismo motivo que ExpenseShareJpaRepository.deleteByExpenseId. */
  @Modifying
  @Query("delete from GroupDeletionApprovalJpaEntity a where a.requestId = :requestId")
  void deleteByRequestId(UUID requestId);

  @Modifying
  @Query("delete from GroupDeletionApprovalJpaEntity a where a.requestId in :requestIds")
  void deleteByRequestIdIn(Collection<UUID> requestIds);
}
