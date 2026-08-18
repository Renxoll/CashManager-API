package pe.smartcash.cash.profile.application.internal.commandservices;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.smartcash.cash.profile.domain.exception.UserProfileAlreadyRegisteredException;
import pe.smartcash.cash.profile.domain.exception.UserProfileNotFoundException;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfile;
import pe.smartcash.cash.profile.domain.model.aggregates.UserProfileRepository;
import pe.smartcash.cash.profile.domain.model.commands.RegisterUserProfileCommand;
import pe.smartcash.cash.profile.domain.model.commands.UpdateFcmTokenCommand;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;
import pe.smartcash.cash.profile.domain.services.UserProfileCommandService;

@Service
class UserProfileCommandServiceImpl implements UserProfileCommandService {

  private final UserProfileRepository userProfileRepository;
  private final Clock clock;

  UserProfileCommandServiceImpl(UserProfileRepository userProfileRepository, Clock clock) {
    this.userProfileRepository = userProfileRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UserId handle(RegisterUserProfileCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    // register() no es un upsert: si Spring Data hiciera save() directo sobre un id
    // existente, JPA lo tomaría como un merge silencioso y pisaría el perfil ya registrado.
    if (userProfileRepository.findById(userId).isPresent()) {
      throw new UserProfileAlreadyRegisteredException(userId);
    }
    UserProfile userProfile = UserProfile.register(userId, command.displayName(), clock.instant());
    userProfileRepository.save(userProfile);
    return userId;
  }

  @Override
  @Transactional
  public void handle(UpdateFcmTokenCommand command) {
    UserId userId = UserId.of(UUID.fromString(command.userId()));
    UserProfile userProfile = userProfileRepository.findById(userId).orElseThrow(() -> new UserProfileNotFoundException(userId));
    userProfile.updateFcmToken(command.fcmToken(), clock.instant());
    userProfileRepository.save(userProfile);
  }
}
