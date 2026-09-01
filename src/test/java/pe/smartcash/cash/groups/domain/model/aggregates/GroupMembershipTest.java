package pe.smartcash.cash.groups.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

class GroupMembershipTest {

  private final GroupId groupId = GroupId.of(UUID.randomUUID());
  private final UserId userId = UserId.of(UUID.randomUUID());

  @Test
  void shouldStartAsPendingWhenInvited() {
    GroupMembership membership = GroupMembership.invite(MembershipId.newId(), groupId, userId, Instant.now());

    assertThat(membership.status()).isEqualTo(MembershipStatus.PENDING);
    assertThat(membership.isAccepted()).isFalse();
  }

  @Test
  void ownerMembershipShouldStartAsAccepted() {
    GroupMembership membership = GroupMembership.ownerMembership(MembershipId.newId(), groupId, userId, Instant.now());

    assertThat(membership.status()).isEqualTo(MembershipStatus.ACCEPTED);
    assertThat(membership.isAccepted()).isTrue();
    assertThat(membership.respondedAt()).isEqualTo(membership.invitedAt());
  }

  @Test
  void shouldAcceptAPendingInvite() {
    GroupMembership membership = GroupMembership.invite(MembershipId.newId(), groupId, userId, Instant.now());

    membership.accept(Instant.now());

    assertThat(membership.status()).isEqualTo(MembershipStatus.ACCEPTED);
    assertThat(membership.isAccepted()).isTrue();
  }

  @Test
  void shouldDeclineAPendingInvite() {
    GroupMembership membership = GroupMembership.invite(MembershipId.newId(), groupId, userId, Instant.now());

    membership.decline(Instant.now());

    assertThat(membership.status()).isEqualTo(MembershipStatus.DECLINED);
    assertThat(membership.isAccepted()).isFalse();
  }

  @Test
  void shouldRejectAcceptingAnAlreadyAcceptedInvite() {
    GroupMembership membership = GroupMembership.invite(MembershipId.newId(), groupId, userId, Instant.now());
    membership.accept(Instant.now());

    assertThatThrownBy(() -> membership.accept(Instant.now())).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldRejectDecliningAnAlreadyDeclinedInvite() {
    GroupMembership membership = GroupMembership.invite(MembershipId.newId(), groupId, userId, Instant.now());
    membership.decline(Instant.now());

    assertThatThrownBy(() -> membership.decline(Instant.now())).isInstanceOf(IllegalStateException.class);
  }
}
