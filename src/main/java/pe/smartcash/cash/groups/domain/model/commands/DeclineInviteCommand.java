package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record DeclineInviteCommand(MembershipId membershipId, UserId requestingUserId) {}
