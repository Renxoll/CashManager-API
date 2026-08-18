package pe.smartcash.cash.profile.interfaces.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.model.events.AccountRegisteredEvent;
import pe.smartcash.cash.profile.domain.model.commands.RegisterUserProfileCommand;
import pe.smartcash.cash.profile.domain.services.UserProfileCommandService;

/**
 * ACL de entrada: Profile escucha el evento de dominio que IAM emite al registrar
 * credenciales nuevas y traduce el hecho externo a su propio comando interno. Vive en
 * {@code interfaces} (no en application) porque es exactamente eso — un borde de entrada al
 * bounded context, análogo a un controller REST, salvo que el "request" es un evento en
 * proceso en vez de un HTTP call.
 *
 * <p>{@code @EventListener} (a diferencia de {@code @TransactionalEventListener}) se ejecuta
 * de forma síncrona en el mismo hilo que publica el evento, mientras la transacción de IAM
 * sigue abierta: {@link UserProfileCommandService#handle} es {@code @Transactional} con
 * propagación REQUIRED por defecto, así que se une a esa misma transacción en vez de abrir
 * una nueva. Si el registro del perfil falla, todo el sign-up (credenciales incluidas) hace
 * rollback — el onboarding queda todo o nada.
 */
@Component
class AccountRegisteredEventHandler {

  private final UserProfileCommandService userProfileCommandService;

  AccountRegisteredEventHandler(UserProfileCommandService userProfileCommandService) {
    this.userProfileCommandService = userProfileCommandService;
  }

  @EventListener
  void on(AccountRegisteredEvent event) {
    userProfileCommandService.handle(new RegisterUserProfileCommand(event.userId().toString(), event.displayName()));
  }
}
