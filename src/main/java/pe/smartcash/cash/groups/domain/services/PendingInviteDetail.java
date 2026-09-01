package pe.smartcash.cash.groups.domain.services;

import java.time.Instant;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;

public record PendingInviteDetail(MembershipId membershipId, GroupId groupId, String groupName, Instant invitedAt) {}
