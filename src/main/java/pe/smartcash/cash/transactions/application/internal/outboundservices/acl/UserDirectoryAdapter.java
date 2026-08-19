package pe.smartcash.cash.transactions.application.internal.outboundservices.acl;

import java.util.Optional;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByInboxAddressQuery;
import pe.smartcash.cash.profile.domain.services.UserProfileDetail;
import pe.smartcash.cash.profile.domain.services.UserProfileQueryService;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;
import pe.smartcash.cash.transactions.domain.services.NotificationTarget;
import pe.smartcash.cash.transactions.domain.services.UserDirectory;

/**
 * Anti-Corruption Layer entre los bounded contexts Transactions y Profile: es el ÚNICO
 * punto donde este contexto habla con Profile, y lo hace solo a través de su API pública de
 * dominio ({@link UserProfileQueryService}), nunca importando {@code profile.domain.model.*}.
 * Traduce el {@code UserId} de este contexto al de Profile en el borde. Vive en application
 * (no en infrastructure): no hace I/O técnico real, solo invoca otro Spring bean en el
 * mismo proceso — es orquestación entre contextos, no un adaptador de infraestructura.
 */
@Component
class UserDirectoryAdapter implements UserDirectory {

  private final UserProfileQueryService userProfileQueryService;

  UserDirectoryAdapter(UserProfileQueryService userProfileQueryService) {
    this.userProfileQueryService = userProfileQueryService;
  }

  @Override
  public boolean exists(UserId userId) {
    return userProfileQueryService.exists(userId.value());
  }

  @Override
  public Optional<NotificationTarget> findNotificationTarget(UserId userId) {
    return findProfile(userId)
        .map(UserProfileDetail::fcmToken)
        .filter(token -> token != null && !token.isBlank())
        .map(NotificationTarget::new);
  }

  @Override
  public Optional<UserId> findUserIdByInboxAddress(String inboxAddress) {
    return userProfileQueryService
        .handle(new FindUserProfileByInboxAddressQuery(inboxAddress))
        .map(UserProfileDetail::userId)
        .map(profileUserId -> UserId.of(profileUserId.value()));
  }

  private Optional<UserProfileDetail> findProfile(UserId userId) {
    var profileUserId = pe.smartcash.cash.profile.domain.model.valueobjects.UserId.of(userId.value());
    return userProfileQueryService.handle(new FindUserProfileByIdQuery(profileUserId));
  }
}
