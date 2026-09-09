package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Un miembro aprueba la solicitud de borrado abierta del grupo. Si con esto ya aprobaron
 * todos, el grupo se borra en cascada. */
public record ApproveGroupDeletionCommand(GroupId groupId, UserId requestingUserId) {}
