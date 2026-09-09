package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.smartcash.cash.groups.infrastructure.persistence.GroupMembershipJpaEntity;

public interface GroupMembershipJpaRepository extends JpaRepository<GroupMembershipJpaEntity, UUID> {

  Optional<GroupMembershipJpaEntity> findByGroupIdAndUserId(UUID groupId, UUID userId);

  List<GroupMembershipJpaEntity> findAllByGroupId(UUID groupId);

  List<GroupMembershipJpaEntity> findAllByUserIdAndStatus(UUID userId, String status);

  /** Parte del borrado en cascada del grupo por consenso. */
  @Modifying
  @Query("delete from GroupMembershipJpaEntity m where m.groupId = :groupId")
  void deleteByGroupId(UUID groupId);
}
