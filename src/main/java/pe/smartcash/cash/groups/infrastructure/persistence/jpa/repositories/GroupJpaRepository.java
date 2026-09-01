package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.smartcash.cash.groups.infrastructure.persistence.GroupJpaEntity;

public interface GroupJpaRepository extends JpaRepository<GroupJpaEntity, UUID> {

  @Query(
      """
      SELECT g FROM GroupJpaEntity g
      WHERE g.id IN (
        SELECT m.groupId FROM GroupMembershipJpaEntity m
        WHERE m.userId = :userId AND m.status = 'ACCEPTED'
      )
      """)
  List<GroupJpaEntity> findAllByMemberUserId(@Param("userId") UUID userId);
}
