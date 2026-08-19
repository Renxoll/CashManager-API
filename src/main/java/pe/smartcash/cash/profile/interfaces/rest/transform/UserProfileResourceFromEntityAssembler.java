package pe.smartcash.cash.profile.interfaces.rest.transform;

import pe.smartcash.cash.profile.domain.services.UserProfileDetail;
import pe.smartcash.cash.profile.interfaces.rest.resources.UserProfileResource;

public final class UserProfileResourceFromEntityAssembler {

  private UserProfileResourceFromEntityAssembler() {}

  public static UserProfileResource toResourceFromEntity(UserProfileDetail detail) {
    return new UserProfileResource(
        detail.userId().value(),
        detail.displayName(),
        detail.fcmToken(),
        detail.inboxAddress(),
        detail.createdAt(),
        detail.updatedAt());
  }
}
