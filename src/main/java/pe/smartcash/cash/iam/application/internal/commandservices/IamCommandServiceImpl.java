package pe.smartcash.cash.iam.application.internal.commandservices;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.iam.domain.exception.EmailAlreadyRegisteredException;
import pe.smartcash.cash.iam.domain.exception.InvalidCredentialsException;
import pe.smartcash.cash.iam.domain.exception.InvalidPasswordResetTokenException;
import pe.smartcash.cash.iam.domain.model.aggregates.Credentials;
import pe.smartcash.cash.iam.domain.model.aggregates.CredentialsRepository;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetToken;
import pe.smartcash.cash.iam.domain.model.aggregates.PasswordResetTokenRepository;
import pe.smartcash.cash.iam.domain.model.commands.LogoutCommand;
import pe.smartcash.cash.iam.domain.model.commands.RefreshTokenCommand;
import pe.smartcash.cash.iam.domain.model.commands.RequestPasswordResetCommand;
import pe.smartcash.cash.iam.domain.model.commands.ResetPasswordCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignInCommand;
import pe.smartcash.cash.iam.domain.model.commands.SignUpCommand;
import pe.smartcash.cash.iam.domain.model.events.AccountRegisteredEvent;
import pe.smartcash.cash.iam.domain.model.valueobjects.Email;
import pe.smartcash.cash.iam.domain.model.valueobjects.HashedPassword;
import pe.smartcash.cash.iam.domain.model.valueobjects.PasswordResetTokenId;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.IamCommandService;
import pe.smartcash.cash.iam.domain.services.PasswordHasher;
import pe.smartcash.cash.iam.domain.services.PasswordResetEmailNotifier;
import pe.smartcash.cash.iam.domain.services.PasswordResetTokenGenerator;
import pe.smartcash.cash.iam.domain.services.TokenBlacklistService;
import pe.smartcash.cash.iam.domain.services.TokenPair;
import pe.smartcash.cash.iam.domain.services.TokenService;

@Slf4j
@Service
class IamCommandServiceImpl implements IamCommandService {

  private final CredentialsRepository credentialsRepository;
  private final PasswordHasher passwordHasher;
  private final TokenService tokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordResetTokenGenerator passwordResetTokenGenerator;
  private final PasswordResetEmailNotifier passwordResetEmailNotifier;
  private final String passwordResetLinkBaseUrl;
  private final Duration passwordResetTtl;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  IamCommandServiceImpl(
      CredentialsRepository credentialsRepository,
      PasswordHasher passwordHasher,
      TokenService tokenService,
      TokenBlacklistService tokenBlacklistService,
      PasswordResetTokenRepository passwordResetTokenRepository,
      PasswordResetTokenGenerator passwordResetTokenGenerator,
      PasswordResetEmailNotifier passwordResetEmailNotifier,
      @Value("${app.password-reset.link-base-url}") String passwordResetLinkBaseUrl,
      @Value("${app.password-reset.ttl}") Duration passwordResetTtl,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.credentialsRepository = credentialsRepository;
    this.passwordHasher = passwordHasher;
    this.tokenService = tokenService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.passwordResetTokenGenerator = passwordResetTokenGenerator;
    this.passwordResetEmailNotifier = passwordResetEmailNotifier;
    this.passwordResetLinkBaseUrl = passwordResetLinkBaseUrl;
    this.passwordResetTtl = passwordResetTtl;
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

  /**
   * Anti-enumeración: se resuelve exactamente igual (sin excepción, sin variar el tiempo de
   * forma observable) exista o no una cuenta con ese email, o sea el email inválido. El
   * único efecto visible del "sí existe" es que llega un correo -- que el atacante no ve.
   *
   * <p>Sin {@code @Transactional} a propósito: el envío a SendGrid no debe correr con una
   * conexión de BD tomada. El borrado de tokens previos ya lleva su propia transacción y el
   * {@code save} es atómico; un corte entre ambos deja al usuario sin token válido y vuelve
   * a pedirlo, sin daño.
   */
  @Override
  public void handle(RequestPasswordResetCommand command) {
    Email email;
    try {
      email = new Email(command.email());
    } catch (IllegalArgumentException malformed) {
      return;
    }
    Credentials credentials = credentialsRepository.findByEmail(email).orElse(null);
    if (credentials == null) {
      return;
    }
    // Solo el último enlace queda vivo: pedir uno nuevo invalida los anteriores.
    passwordResetTokenRepository.deleteAllByUserId(credentials.id());
    PasswordResetTokenGenerator.GeneratedToken generated = passwordResetTokenGenerator.generate();
    PasswordResetToken token =
        PasswordResetToken.issue(
            PasswordResetTokenId.newId(), credentials.id(), generated.tokenHash(), clock.instant(), passwordResetTtl);
    passwordResetTokenRepository.save(token);

    String resetUrl = passwordResetLinkBaseUrl + "?token=" + generated.rawToken();
    passwordResetEmailNotifier.sendResetLink(email, resetUrl, passwordResetTtl);
  }

  @Override
  @Transactional
  public void handle(ResetPasswordCommand command) {
    String tokenHash = passwordResetTokenGenerator.hash(command.rawToken());
    PasswordResetToken token =
        passwordResetTokenRepository
            .findByTokenHash(tokenHash)
            .filter(t -> t.isRedeemable(clock.instant()))
            .orElseThrow(InvalidPasswordResetTokenException::new);

    Credentials credentials =
        credentialsRepository
            .findById(token.userId())
            // La cuenta se borró entre la emisión del enlace y su uso: mismo error genérico.
            .orElseThrow(InvalidPasswordResetTokenException::new);

    credentials.changePassword(passwordHasher.hash(command.newRawPassword()));
    credentialsRepository.save(credentials);

    token.redeem(clock.instant());
    passwordResetTokenRepository.save(token);
    // Nota: las sesiones ya emitidas (access/refresh tokens HMAC stateless) siguen válidas
    // hasta expirar -- el esquema de tokens actual no tiene revocación por-usuario. El
    // access token vence en minutos; el refresh dura más, asumido como aceptable para el MVP.
    log.info("Contraseña restablecida para el usuario {}", credentials.id().value());
  }
}
