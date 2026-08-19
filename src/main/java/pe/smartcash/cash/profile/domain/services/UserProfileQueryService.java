package pe.smartcash.cash.profile.domain.services;

import java.util.Optional;
import java.util.UUID;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByInboxAddressQuery;

public interface UserProfileQueryService {

  Optional<UserProfileDetail> handle(FindUserProfileByIdQuery query);

  /** Resuelve el dueño de un correo entrante (ver ingesta por SendGrid Inbound Parse). */
  Optional<UserProfileDetail> handle(FindUserProfileByInboxAddressQuery query);

  /** Chequeo liviano de existencia, pensado para el ACL de otros bounded contexts. */
  boolean exists(UUID userId);
}
