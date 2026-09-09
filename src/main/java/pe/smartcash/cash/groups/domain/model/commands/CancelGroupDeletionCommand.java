package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/** Cualquier miembro aceptado puede cancelar la solicitud de borrado mientras siga PENDING. */
public record CancelGroupDeletionCommand(GroupId groupId, UserId requestingUserId) {}
