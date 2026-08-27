package pe.smartcash.cash.gmailsync.infrastructure.persistence;

import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.gmailsync.domain.model.aggregates.GmailConnection;
import pe.smartcash.cash.gmailsync.domain.model.valueobjects.UserId;
import pe.smartcash.cash.gmailsync.infrastructure.crypto.TokenCipher;

/**
 * A diferencia de los demás mappers del proyecto (funciones estáticas puras), este necesita
 * {@link TokenCipher} inyectado -- cifra/descifra los tokens exactamente en el borde entre
 * dominio (los ve como String opaco) y persistencia (nunca los ve en texto plano).
 */
@Component
class GmailConnectionEntityMapper {

  private final TokenCipher tokenCipher;

  GmailConnectionEntityMapper(TokenCipher tokenCipher) {
    this.tokenCipher = tokenCipher;
  }

  GmailConnectionJpaEntity toJpaEntity(GmailConnection connection, UUID existingId) {
    return GmailConnectionJpaEntity.builder()
        .id(existingId != null ? existingId : UUID.randomUUID())
        .userId(connection.userId().value())
        .accessToken(tokenCipher.encrypt(connection.accessToken()))
        .refreshToken(tokenCipher.encrypt(connection.refreshToken()))
        .accessTokenExpiresAt(connection.accessTokenExpiresAt())
        .lastSyncedAt(connection.lastSyncedAt())
        .connectedAt(connection.connectedAt())
        .updatedAt(connection.updatedAt())
        .build();
  }

  GmailConnection toDomain(GmailConnectionJpaEntity entity) {
    return GmailConnection.rehydrate(
        UserId.of(entity.getUserId()),
        tokenCipher.decrypt(entity.getAccessToken()),
        tokenCipher.decrypt(entity.getRefreshToken()),
        entity.getAccessTokenExpiresAt(),
        entity.getLastSyncedAt(),
        entity.getConnectedAt(),
        entity.getUpdatedAt());
  }
}
