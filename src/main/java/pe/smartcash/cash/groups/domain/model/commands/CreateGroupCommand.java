package pe.smartcash.cash.groups.domain.model.commands;

import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record CreateGroupCommand(UserId requestingUserId, String name) {}
