package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Un miembro propone borrar el grupo -- queda como primer aprobador. */
public record RequestGroupDeletionCommand(GroupId groupId, UserId requestingUserId) {}
