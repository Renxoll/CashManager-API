package pe.smartcash.cash.iam.application.internal.queryservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.queries.FindUserIdByEmailQuery;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/** Fake en memoria en vez de un mock -- no hay ningún uso de Mockito para unit tests puros
 * en este proyecto (solo @MockitoBean dentro de tests de integración con Spring), así que
 * se sigue el mismo criterio acá: objeto real, no framework de mocking. */
class IamQueryServiceImplTest {

  private final Map<String, Credentials> byEmail = new HashMap<>();
  private final CredentialsRepository fakeRepository =
      new CredentialsRepository() {
        @Override
        public Optional<Credentials> findByEmail(Email email) {
          return Optional.ofNullable(byEmail.get(email.value()));
        }

        @Override
        public Optional<Credentials> findById(UserId id) {
          throw new UnsupportedOperationException("no usado en este test");
        }

        @Override
        public void save(Credentials credentials) {
          throw new UnsupportedOperationException("no usado en este test");
        }
      };

  private final IamQueryServiceImpl service = new IamQueryServiceImpl(fakeRepository);

  @Test
  void shouldReturnUserIdWhenEmailIsRegistered() {
    UserId userId = UserId.of(UUID.randomUUID());
    Email email = new Email("existe@example.com");
    byEmail.put(email.value(), Credentials.register(userId, email, new HashedPassword("hash"), Instant.now()));

    Optional<UserId> result = service.handle(new FindUserIdByEmailQuery("existe@example.com"));

    assertThat(result).contains(userId);
  }

  @Test
  void shouldReturnEmptyWhenEmailIsNotRegistered() {
    Optional<UserId> result = service.handle(new FindUserIdByEmailQuery("nadie@example.com"));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyForMalformedEmailInsteadOfThrowing() {
    Optional<UserId> result = service.handle(new FindUserIdByEmailQuery("esto-no-es-un-email"));

    assertThat(result).isEmpty();
  }
}
