package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record GroupSummaryResource(UUID groupId, String name, int memberCount, List<CurrencyBalanceResource> yourBalances) {}
