package pe.smartcash.cash.groups.domain.services;

import java.util.Optional;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;

/**
 * Puerto (Anti-Corruption Layer) hacia los bounded contexts IAM y Profile: resuelve el
 * email de un invitado a una cuenta real (IAM) y el nombre a mostrar de un miembro
 * (Profile). Mismo rol que {@code transactions.domain.services.UserDirectory}, puerto
 * independiente y propio de este contexto.
 */
public interface UserDirectory {

  /** Vacío si no existe ninguna cuenta con ese email -- ver InvitedUserNotRegisteredException. */
  Optional<UserId> findUserIdByEmail(String email);

  Optional<String> findDisplayName(UserId userId);
}
