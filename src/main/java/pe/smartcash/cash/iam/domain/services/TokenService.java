package pe.smartcash.cash.iam.domain.services;

import java.util.Optional;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/**
 * Puerto: emitir y validar el token que protege la API. La implementación por defecto
 * (infrastructure.tokens) emite JWT firmados con HS256 vía JJWT — mismo contrato, así que
 * cambiar de librería o de algoritmo más adelante es un adaptador nuevo, no un cambio de
 * dominio.
 */
public interface TokenService {

  AccessToken issue(UserId userId);

  Optional<UserId> validate(String tokenValue);
}
