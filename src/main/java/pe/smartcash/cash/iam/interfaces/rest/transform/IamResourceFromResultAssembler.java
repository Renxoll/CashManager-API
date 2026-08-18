package pe.smartcash.cash.iam.interfaces.rest.transform;

import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.AccessToken;
import pe.smartcash.cash.iam.interfaces.rest.resources.AccessTokenResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResultResource;

public final class IamResourceFromResultAssembler {

  private IamResourceFromResultAssembler() {}

  public static SignUpResultResource toSignUpResultResource(UserId userId) {
    return new SignUpResultResource(userId.value());
  }

  public static AccessTokenResource toAccessTokenResource(AccessToken accessToken) {
    return new AccessTokenResource(accessToken.value(), accessToken.expiresAt());
  }
}
