package pe.smartcash.cash.iam.interfaces.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.iam.domain.exception.InvalidCredentialsException;
import pe.smartcash.cash.iam.domain.services.IamCommandService;
import pe.smartcash.cash.iam.interfaces.rest.resources.RefreshTokenResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignInResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResultResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.TokenPairResource;
import pe.smartcash.cash.iam.interfaces.rest.transform.IamCommandFromResourceAssembler;
import pe.smartcash.cash.iam.interfaces.rest.transform.IamResourceFromResultAssembler;

@RestController
@RequestMapping("/api/v1/iam")
class IamController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final IamCommandService iamCommandService;

  IamController(IamCommandService iamCommandService) {
    this.iamCommandService = iamCommandService;
  }

  @PostMapping("/sign-up")
  ResponseEntity<SignUpResultResource> signUp(@Valid @RequestBody SignUpResource resource) {
    var userId = iamCommandService.handle(IamCommandFromResourceAssembler.toSignUpCommand(resource));
    return ResponseEntity.status(HttpStatus.CREATED).body(IamResourceFromResultAssembler.toSignUpResultResource(userId));
  }

  @PostMapping("/sign-in")
  ResponseEntity<TokenPairResource> signIn(@Valid @RequestBody SignInResource resource) {
    var tokenPair = iamCommandService.handle(IamCommandFromResourceAssembler.toSignInCommand(resource));
    return ResponseEntity.ok(IamResourceFromResultAssembler.toTokenPairResource(tokenPair));
  }

  @PostMapping("/refresh")
  ResponseEntity<TokenPairResource> refresh(@Valid @RequestBody RefreshTokenResource resource) {
    var tokenPair = iamCommandService.handle(IamCommandFromResourceAssembler.toRefreshTokenCommand(resource));
    return ResponseEntity.ok(IamResourceFromResultAssembler.toTokenPairResource(tokenPair));
  }

  /**
   * No pide el token en el body: lo toma del mismo header {@code Authorization} que ya
   * trajo el request hasta acá (idéntico criterio a los demás endpoints autenticados de la
   * API — nunca se le pide al cliente algo que el propio request ya está transportando).
   */
  @PostMapping("/logout")
  ResponseEntity<Void> logout(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      throw new InvalidCredentialsException();
    }
    String accessToken = header.substring(BEARER_PREFIX.length());
    iamCommandService.handle(IamCommandFromResourceAssembler.toLogoutCommand(accessToken));
    return ResponseEntity.noContent().build();
  }
}
