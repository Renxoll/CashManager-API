package pe.smartcash.cash.groups.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.groups.domain.model.valueobjects.DeletionRequestStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupDeletionRequestId;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

class GroupDeletionRequestTest {

  private final GroupId groupId = GroupId.of(UUID.randomUUID());
  private final UserId alice = UserId.of(UUID.randomUUID());
  private final UserId bob = UserId.of(UUID.randomUUID());
  private final UserId carol = UserId.of(UUID.randomUUID());

  @Test
  void openCountsTheRequesterAsFirstApprover() {
    GroupDeletionRequest request = GroupDeletionRequest.open(GroupDeletionRequestId.newId(), groupId, alice, Instant.now());

    assertThat(request.status()).isEqualTo(DeletionRequestStatus.PENDING);
    assertThat(request.approvedBy()).containsExactly(alice);
    assertThat(request.isApprovedByAll(Set.of(alice))).isTrue();
    assertThat(request.isApprovedByAll(Set.of(alice, bob))).isFalse();
  }

  @Test
  void isApprovedByAllOnlyWhenEveryAcceptedMemberVoted() {
    GroupDeletionRequest request = GroupDeletionRequest.open(GroupDeletionRequestId.newId(), groupId, alice, Instant.now());
    request.approve(bob, Instant.now());

    assertThat(request.isApprovedByAll(Set.of(alice, bob))).isTrue();
    assertThat(request.isApprovedByAll(Set.of(alice, bob, carol))).isFalse();
  }

  @Test
  void approveIsIdempotentPerUser() {
    GroupDeletionRequest request = GroupDeletionRequest.open(GroupDeletionRequestId.newId(), groupId, alice, Instant.now());
    request.approve(bob, Instant.now());
    request.approve(bob, Instant.now());

    assertThat(request.approvedBy()).containsExactlyInAnyOrder(alice, bob);
  }

  @Test
  void cannotApproveOrCancelOnceCompleted() {
    GroupDeletionRequest request = GroupDeletionRequest.open(GroupDeletionRequestId.newId(), groupId, alice, Instant.now());
    request.markCompleted(Instant.now());

    assertThat(request.status()).isEqualTo(DeletionRequestStatus.COMPLETED);
    assertThatThrownBy(() -> request.approve(bob, Instant.now())).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> request.cancel(Instant.now())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void cancelResolvesTheRequest() {
    GroupDeletionRequest request = GroupDeletionRequest.open(GroupDeletionRequestId.newId(), groupId, alice, Instant.now());
    Instant cancelledAt = Instant.now();
    request.cancel(cancelledAt);

    assertThat(request.status()).isEqualTo(DeletionRequestStatus.CANCELLED);
    assertThat(request.resolvedAt()).isEqualTo(cancelledAt);
  }
}
