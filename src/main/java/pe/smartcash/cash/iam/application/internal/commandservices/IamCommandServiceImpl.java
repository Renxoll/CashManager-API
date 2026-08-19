package pe.smartcash.cash.iam.application.internal.commandservices;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.iam.domain.exception.EmailAlreadyRegisteredException;
import pe.smartcash.cash.iam.domain.exception.InvalidCredentialsException;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.commands.LogoutCommand;
import pe.smartcash.cash.iam.domain.model.commands.RefreshTokenCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignInCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignUpCommand;
import pe.smartcash.cash.iam.domain.model.events.AccountRegisteredEvent;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.IamCommandService;
import pe.smartcash.cash.iam.domain.services.PasswordHasher;
import pe.smartcash.cash.iam.domain.services.TokenBlacklistService;
import pe.smartcash.cash.iam.domain.services.TokenPair;
import pe.smartcash.cash.iam.domain.services.TokenService;

@Service
class IamCommandServiceImpl implements IamCommandService {

  private final CredentialsRepository credentialsRepository;
  private final PasswordHasher passwordHasher;
  private final TokenService tokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  IamCommandServiceImpl(
      CredentialsRepository credentialsRepository,
      PasswordHasher passwordHasher,
      TokenService tokenService,
      TokenBlacklistService tokenBlacklistService,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.credentialsRepository = credentialsRepository;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.eventPublisher = eventPublisher;
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
    // @EventListener (no @TransactionalEventListener) corre síncrono, dentro de esta misma
    // transacción @Transactional: si Profile falla al crear el perfil, todo el sign-up
    // (incluidas las credenciales) hace rollback — onboarding atómico entre bounded contexts.
    eventPublisher.publishEvent(new AccountRegisteredEvent(credentials.id().value(), email.value(), command.displayName()));
    return credentials.id();
  }

  @Override
  public TokenPair handle(SignInCommand command) {
    Email email = new Email(command.email());
    Credentials credentials = credentialsRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    if (!passwordHasher.matches(command.rawPassword(), credentials.hashedPassword())) {
      throw new InvalidCredentialsException();
    }
    return tokenService.issue(credentials.id());
  }

  @Override
  public TokenPair handle(RefreshTokenCommand command) {
    // Reusa InvalidCredentialsException (ya mapeada a 401 en IamExceptionHandler): un
    // refresh token vencido, con firma inválida, o que en realidad es un access token
    // disfrazado, es el mismo caso semántico que "no sé quién sos".
    UserId userId = tokenService.validateRefreshToken(command.refreshToken()).orElseThrow(InvalidCredentialsException::new);
    return tokenService.issue(userId);
  }

  @Override
  public void handle(LogoutCommand command) {
    Instant expiresAt = tokenService.expiresAt(command.accessToken()).orElseThrow(InvalidCredentialsException::new);
    Duration remainingTtl = Duration.between(clock.instant(), expiresAt);
    if (remainingTtl.isPositive()) {
      tokenBlacklistService.blacklist(command.accessToken(), remainingTtl);
    }
    // Si ya no queda tiempo de vida (remainingTtl <= 0), no hace falta blacklistearlo: el
    // propio TokenService ya lo va a rechazar por expirado en la próxima validación.
  }
}
