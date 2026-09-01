package pe.smartcash.cash.groups.domain.services;

import java.util.List;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipId;
import pe.smartcash.cash.groups.domain.model.valueobjects.MembershipStatus;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

public record GroupMemberDetail(
    MembershipId membershipId, UserId userId, String displayName, MembershipStatus status, List<CurrencyBalance> balances) {}
