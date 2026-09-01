package pe.smartcash.cash.groups.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record PendingInviteResource(UUID membershipId, UUID groupId, String groupName, Instant invitedAt) {}
