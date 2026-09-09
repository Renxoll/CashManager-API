package pe.smartcash.cash.groups.infrastructure.persistence.jpa.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pe.smartcash.cash.groups.infrastructure.persistence.GroupDeletionRequestJpaEntity;

public interface GroupDeletionRequestJpaRepository extends JpaRepository<GroupDeletionRequestJpaEntity, UUID> {

  Optional<GroupDeletionRequestJpaEntity> findByGroupIdAndStatus(UUID groupId, String status);

  @Query("select r.id from GroupDeletionRequestJpaEntity r where r.groupId = :groupId")
  List<UUID> findIdsByGroupId(UUID groupId);

  /** Parte del borrado en cascada del grupo -- las aprobaciones se borran aparte, antes que esto. */
  @Modifying
  @Query("delete from GroupDeletionRequestJpaEntity r where r.groupId = :groupId")
  void deleteByGroupId(UUID groupId);
}
