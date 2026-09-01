package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record InviteMemberCommand(GroupId groupId, UserId requestingUserId, String inviteeEmail) {}
