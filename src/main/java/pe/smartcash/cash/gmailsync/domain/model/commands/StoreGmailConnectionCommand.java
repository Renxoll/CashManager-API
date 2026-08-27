package pe.smartcash.cash.gmailsync.domain.model.commands;

import java.time.Instant;

/**
 * String userId (no un value object) a propósito: el caller (el callback OAuth, ver
 * {@code GmailOAuthController}) resuelve el id de sesión antes de llegar acá, y este
 * contexto no debe conocer el tipo interno de UserId de ningún otro módulo -- mismo
 * criterio que {@code transactions.IngestBankNotificationCommand}.
 */
public record StoreGmailConnectionCommand(
    String userId, String accessToken, String refreshToken, Instant accessTokenExpiresAt) {}
