package pe.smartcash.cash.profile.interfaces.rest.transform;

import pe.smartcash.cash.profile.domain.model.commands.RegisterUserProfileCommand;
import pe.smartcash.cash.profile.domain.model.commands.UpdateFcmTokenCommand;
import pe.smartcash.cash.profile.interfaces.rest.resources.RegisterUserProfileResource;
import pe.smartcash.cash.profile.interfaces.rest.resources.UpdateFcmTokenResource;

public final class UserProfileCommandFromResourceAssembler {

  private UserProfileCommandFromResourceAssembler() {}

  public static RegisterUserProfileCommand toRegisterCommand(String authenticatedUserId, RegisterUserProfileResource resource) {
    return new RegisterUserProfileCommand(authenticatedUserId, resource.displayName());
  }

  public static UpdateFcmTokenCommand toUpdateFcmTokenCommand(String authenticatedUserId, UpdateFcmTokenResource resource) {
    return new UpdateFcmTokenCommand(authenticatedUserId, resource.fcmToken());
  }
}
