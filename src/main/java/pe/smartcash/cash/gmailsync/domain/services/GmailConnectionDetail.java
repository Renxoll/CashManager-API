package pe.smartcash.cash.gmailsync.domain.services;

import java.time.Instant;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;

public record GmailConnectionDetail(GmailConnectionId id, String email, Instant connectedAt, Instant lastSyncedAt) {}
