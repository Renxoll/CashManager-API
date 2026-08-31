package pe.smartcash.cash.gmailsync.interfaces.rest.resources;

import java.time.Instant;

/**
 * Respuesta del refresco manual de Gmail ({@code POST /api/v1/gmail/connections/sync}).
 * {@code transactionsIngested} son gastos nuevos que ya entraron a la cola de categorización;
 * {@code pendingSendersRegistered} son correos de remitentes que el usuario todavía tiene
 * que aprobar antes de que cuenten como gasto.
 */
public record GmailSyncResultResource(
    int connectionsSynced, int transactionsIngested, int pendingSendersRegistered, Instant syncedAt) {}
