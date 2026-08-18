package pe.smartcash.cash.iam.application.internal.commandservices;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.iam.domain.exception.EmailAlreadyRegisteredException;
import pe.smartcash.cash.iam.domain.exception.InvalidCredentialsException;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.commands.SignInCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignUpCommand;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.AccessToken;
import pe.smartcash.cash.iam.domain.services.IamCommandService;
import pe.smartcash.cash.iam.domain.services.PasswordHasher;
import pe.smartcash.cash.iam.domain.services.TokenService;

@Service
class IamCommandServiceImpl implements IamCommandService {

  private final CredentialsRepository credentialsRepository;
  private final PasswordHasher passwordHasher;
  private final TokenService tokenService;
  private final Clock clock;

  IamCommandServiceImpl(
      CredentialsRepository credentialsRepository, PasswordHasher passwordHasher, TokenService tokenService, Clock clock) {
    this.credentialsRepository = credentialsRepository;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UserId handle(SignUpCommand command) {
    Email email = new Email(command.email());
    if (credentialsRepository.findByEmail(email).isPresent()) {
      throw new EmailAlreadyRegisteredException(email);
    }
    HashedPassword hashedPassword = passwordHasher.hash(command.rawPassword());
    Credentials credentials = Credentials.register(UserId.newId(), email, hashedPassword, clock.instant());
    credentialsRepository.save(credentials);
    return credentials.id();
  }

  @Override
  public AccessToken handle(SignInCommand command) {
    Email email = new Email(command.email());
    Credentials credentials = credentialsRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    if (!passwordHasher.matches(command.rawPassword(), credentials.hashedPassword())) {
      throw new InvalidCredentialsException();
    }
    return tokenService.issue(credentials.id());
  }
}
