package pe.smartcash.cash.iam.interfaces.rest.transform;

import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;
import pe.smartcash.cash.iam.domain.services.TokenPair;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResultResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.TokenPairResource;

public final class IamResourceFromResultAssembler {

  private IamResourceFromResultAssembler() {}

  public static SignUpResultResource toSignUpResultResource(UserId userId) {
    return new SignUpResultResource(userId.value());
  }

  public static TokenPairResource toTokenPairResource(TokenPair tokenPair) {
    return new TokenPairResource(
        tokenPair.accessToken().value(),
        tokenPair.accessToken().expiresAt(),
        tokenPair.refreshToken().value(),
        tokenPair.refreshToken().expiresAt());
  }
}
