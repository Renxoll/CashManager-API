package pe.smartcash.cash.iam.domain.model.aggregates;

import java.util.Optional;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

public interface CredentialsRepository {

  Optional<Credentials> findByEmail(Email email);

  Optional<Credentials> findById(UserId id);

  void save(Credentials credentials);
}
