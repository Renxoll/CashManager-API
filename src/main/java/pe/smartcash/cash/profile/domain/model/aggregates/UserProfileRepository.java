package pe.smartcash.cash.profile.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

public interface UserProfileRepository {

  Optional<UserProfile> findById(UserId userId);

  void save(UserProfile userProfile);
}
