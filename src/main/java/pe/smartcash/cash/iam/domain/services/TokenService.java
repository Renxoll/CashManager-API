package pe.smartcash.cash.iam.domain.services;

import java.time.Instant;
import java.util.Optional;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/**
 * Puerto: emitir y validar los tokens que protegen la API. La implementación por defecto
 * (infrastructure.tokens) firma con HMAC-SHA256 nativo (sin librerías externas de JWT) —
 * mismo contrato, así que cambiar de algoritmo más adelante es un adaptador nuevo, no un
 * cambio de dominio. Access y refresh token comparten formato pero llevan un discriminador
 * de tipo embebido: un access token nunca sirve como refresh token ni viceversa, aunque un
 * atacante lo intente presentar donde no corresponde.
 */
public interface TokenService {

  TokenPair issue(UserId userId);

  Optional<UserId> validate(String accessTokenValue);

  Optional<UserId> validateRefreshToken(String refreshTokenValue);

  /** Para calcular el TTL de un blacklist (logout): vencimiento embebido, si el token es válido. */
  Optional<Instant> expiresAt(String tokenValue);
}
