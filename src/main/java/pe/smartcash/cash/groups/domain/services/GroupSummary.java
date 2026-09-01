package pe.smartcash.cash.groups.domain.services;

import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.GroupId;

public record GroupSummary(GroupId groupId, String name, int memberCount, List<CurrencyBalance> yourBalances) {}
