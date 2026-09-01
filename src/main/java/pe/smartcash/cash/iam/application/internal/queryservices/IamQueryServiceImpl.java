package pe.smartcash.cash.iam.application.internal.queryservices;

import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.queries.FindUserIdByEmailQuery;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.IamQueryService;

@Service
class IamQueryServiceImpl implements IamQueryService {

  private final CredentialsRepository credentialsRepository;

  IamQueryServiceImpl(CredentialsRepository credentialsRepository) {
    this.credentialsRepository = credentialsRepository;
  }

  @Override
  public Optional<UserId> handle(FindUserIdByEmailQuery query) {
    Email email;
    try {
      email = new Email(query.email());
    } catch (IllegalArgumentException malformed) {
      // Un email mal formado nunca puede ser el de una cuenta real -- se resuelve igual que
      // "no encontrado" en vez de dejar que la excepción de validación se propague al
      // caller (que solo quiere saber si existe una cuenta, no validar el formato).
      return Optional.empty();
    }
    return credentialsRepository.findByEmail(email).map(Credentials::id);
  }
}
