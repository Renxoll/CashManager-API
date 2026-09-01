package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupDetailResource(
    UUID groupId,
    String name,
    UUID ownerId,
    Instant createdAt,
    List<GroupMemberResource> members,
    List<ExpenseResource> expenses,
    List<SettlementResource> settlements,
    List<SuggestedSettlementResource> simplifiedDebts) {}
