package pe.smartcash.cash.gmailsync.domain.services;

import java.time.Instant;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.GmailConnectionId;

/** {@code needsReconnect} = el grant OAuth dejó de servir (revocado o sin scope); el poll ya
 * no la reintenta y el frontend debe ofrecer reconectar. */
public record GmailConnectionDetail(
    GmailConnectionId id, String email, Instant connectedAt, Instant lastSyncedAt, boolean needsReconnect) {}
