package pe.smartcash.cash.gmailsync.interfaces.rest.resources;

import java.time.Instant;
import java.util.UUID;

public record GmailConnectionResource(UUID id, String email, Instant connectedAt, Instant lastSyncedAt) {}
