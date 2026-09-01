package pe.smartcash.cash.iam.domain.services;

import java.util.Optional;
import pe.smartcash.cash.iam.domain.model.queries.FindUserIdByEmailQuery;
import pe.smartcash.cash.iam.domain.model.valueobjects.UserId;

/**
 * Primera query service de IAM: hasta ahora este contexto solo exponía comandos
 * (sign-up/sign-in/refresh/logout). {@code groups} necesita validar que el email de un
 * invitado corresponda a una cuenta real antes de mandar una invitación -- ver {@code
 * groups.application.internal.outboundservices.acl.UserDirectoryAdapter}.
 */
public interface IamQueryService {

  Optional<UserId> handle(FindUserIdByEmailQuery query);
}
