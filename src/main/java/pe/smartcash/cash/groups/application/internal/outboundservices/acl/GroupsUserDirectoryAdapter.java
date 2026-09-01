package pe.smartcash.cash.groups.application.internal.outboundservices.acl;

import java.util.Optional;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.groups.domain.model.valueobjects.UserId;
import pe.smartcash.cash.groups.domain.services.UserDirectory;
import pe.smartcash.cash.iam.domain.model.queries.FindUserIdByEmailQuery;
import pe.smartcash.cash.iam.domain.services.IamQueryService;
import pe.smartcash.cash.profile.domain.model.queries.FindUserProfileByIdQuery;
import pe.smartcash.cash.profile.domain.services.UserProfileDetail;
import pe.smartcash.cash.profile.domain.services.UserProfileQueryService;

/**
 * Anti-Corruption Layer entre Groups e IAM/Profile: es el único punto donde este contexto
 * habla con esos dos, siempre a través de su API pública de dominio ({@link IamQueryService},
 * {@link UserProfileQueryService}), nunca importando sus internals. Traduce el {@code UserId}
 * de cada contexto al de este en el borde -- mismo patrón que
 * {@code transactions.application.internal.outboundservices.acl.UserDirectoryAdapter}.
 */
@Component
class GroupsUserDirectoryAdapter implements UserDirectory {

  private final IamQueryService iamQueryService;
  private final UserProfileQueryService userProfileQueryService;

  GroupsUserDirectoryAdapter(IamQueryService iamQueryService, UserProfileQueryService userProfileQueryService) {
    this.iamQueryService = iamQueryService;
    this.userProfileQueryService = userProfileQueryService;
  }

  @Override
  public Optional<UserId> findUserIdByEmail(String email) {
    return iamQueryService.handle(new FindUserIdByEmailQuery(email)).map(iamUserId -> UserId.of(iamUserId.value()));
  }

  @Override
  public Optional<String> findDisplayName(UserId userId) {
    var profileUserId = pe.smartcash.cash.profile.domain.model.valueobjects.UserId.of(userId.value());
    return userProfileQueryService.handle(new FindUserProfileByIdQuery(profileUserId)).map(UserProfileDetail::displayName);
  }
}
