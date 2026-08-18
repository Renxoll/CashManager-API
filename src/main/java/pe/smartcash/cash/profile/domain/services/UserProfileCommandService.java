package pe.smartcash.cash.profile.domain.services;

import pe.smartcash.cash.profile.domain.model.commands.RegisterUserProfileCommand;
import pe.smartcash.cash.profile.domain.model.commands.UpdateFcmTokenCommand;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;

public interface UserProfileCommandService {

  UserId handle(RegisterUserProfileCommand command);

  void handle(UpdateFcmTokenCommand command);
}
