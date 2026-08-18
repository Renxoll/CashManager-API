package pe.smartcash.cash.profile.domain.services;

import java.util.Optional;
import java.util.UUID;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;

public interface UserProfileQueryService {

  Optional<UserProfileDetail> handle(FindUserProfileByIdQuery query);

  /** Chequeo liviano de existencia, pensado para el ACL de otros bounded contexts. */
  boolean exists(UUID userId);
}
