package pe.smartcash.cash.iam.domain.services;

import java.time.Duration;

/**
 * Puerto: revocación de access tokens antes de su expiración natural (logout real en una API
 * stateless). La implementación por defecto vive en infrastructure y usa Redis — el dominio
 * no sabe ni le importa dónde se guarda la lista.
 */
public interface TokenBlacklistService {

  void blacklist(String accessTokenValue, Duration ttl);

  boolean isBlacklisted(String accessTokenValue);
}
