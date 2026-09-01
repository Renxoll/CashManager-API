package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

public record GroupMemberResource(
    UUID membershipId, UUID userId, String displayName, String status, List<CurrencyBalanceResource> balances) {}
