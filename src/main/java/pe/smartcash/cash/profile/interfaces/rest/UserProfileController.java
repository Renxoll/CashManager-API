package pe.smartcash.cash.profile.interfaces.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.profile.domain.exception.UserProfileNotFoundException;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;
import pe.smartcash.cash.profile.domain.model.valueobjects.UserId;
import pe.smartcash.cash.profile.domain.services.UserProfileCommandService;
import pe.smartcash.cash.profile.domain.services.UserProfileQueryService;
import pe.smartcash.cash.profile.interfaces.rest.resources.RegisterUserProfileResource;
import pe.smartcash.cash.profile.interfaces.rest.resources.UpdateFcmTokenResource;
import pe.smartcash.cash.profile.interfaces.rest.resources.UserProfileResource;
import pe.smartcash.cash.profile.interfaces.rest.transform.UserProfileCommandFromResourceAssembler;
import pe.smartcash.cash.profile.interfaces.rest.transform.UserProfileResourceFromEntityAssembler;

/**
 * El request ya pasó por {@code BearerTokenAuthenticationFilter} de IAM antes de llegar
 * acá: el {@code userId} nunca se pide en el body ni en el path, se toma del principal ya
 * autenticado ({@link AuthenticationPrincipal}) para que sea imposible que un cliente
 * registre o modifique el perfil de otro usuario suplantando su id.
 */
@RestController
@RequestMapping("/api/v1/profiles")
class UserProfileController {

  private final UserProfileCommandService userProfileCommandService;
  private final UserProfileQueryService userProfileQueryService;

  UserProfileController(UserProfileCommandService userProfileCommandService, UserProfileQueryService userProfileQueryService) {
    this.userProfileCommandService = userProfileCommandService;
    this.userProfileQueryService = userProfileQueryService;
  }

  @PostMapping
  ResponseEntity<UserProfileResource> register(
      @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody RegisterUserProfileResource resource) {
    UserId userId =
        userProfileCommandService.handle(UserProfileCommandFromResourceAssembler.toRegisterCommand(authenticatedUserId, resource));
    return ResponseEntity.status(HttpStatus.CREATED).body(fetch(userId));
  }

  @PutMapping("/fcm-token")
  ResponseEntity<Void> updateFcmToken(
      @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody UpdateFcmTokenResource resource) {
    userProfileCommandService.handle(UserProfileCommandFromResourceAssembler.toUpdateFcmTokenCommand(authenticatedUserId, resource));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  ResponseEntity<UserProfileResource> me(@AuthenticationPrincipal String authenticatedUserId) {
    return ResponseEntity.ok(fetch(UserId.of(UUID.fromString(authenticatedUserId))));
  }

  private UserProfileResource fetch(UserId userId) {
    var detail =
        userProfileQueryService
            .handle(new FindUserProfileByIdQuery(userId))
            .orElseThrow(() -> new UserProfileNotFoundException(userId));
    return UserProfileResourceFromEntityAssembler.toResourceFromEntity(detail);
  }
}
