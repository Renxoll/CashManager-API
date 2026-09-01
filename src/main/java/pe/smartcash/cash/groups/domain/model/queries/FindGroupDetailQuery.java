package pe.smartcash.cash.groups.domain.model.queries;

import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record FindGroupDetailQuery(GroupId groupId, UserId requestingUserId) {}
