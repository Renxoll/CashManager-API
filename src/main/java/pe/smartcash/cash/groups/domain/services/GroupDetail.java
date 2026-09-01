package pe.smartcash.cash.groups.domain.services;

import java.time.Instant;
import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record GroupDetail(
    GroupId groupId,
    String name,
    UserId ownerId,
    Instant createdAt,
    List<GroupMemberDetail> members,
    List<ExpenseDetail> expenses,
    List<SettlementDetail> settlements,
    List<SuggestedSettlementDetail> simplifiedDebts) {}
