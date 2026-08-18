package pe.smartcash.cash.iam.interfaces.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.iam.domain.services.IamCommandService;
import pe.smartcash.cash.iam.interfaces.rest.resources.AccessTokenResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignInResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResource;
import pe.smartcash.cash.iam.interfaces.rest.resources.SignUpResultResource;
import pe.smartcash.cash.iam.interfaces.rest.transform.IamCommandFromResourceAssembler;
import pe.smartcash.cash.iam.interfaces.rest.transform.IamResourceFromResultAssembler;

@RestController
@RequestMapping("/api/v1/iam")
class IamController {

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
  ResponseEntity<AccessTokenResource> signIn(@Valid @RequestBody SignInResource resource) {
    var accessToken = iamCommandService.handle(IamCommandFromResourceAssembler.toSignInCommand(resource));
    return ResponseEntity.ok(IamResourceFromResultAssembler.toAccessTokenResource(accessToken));
  }
}
