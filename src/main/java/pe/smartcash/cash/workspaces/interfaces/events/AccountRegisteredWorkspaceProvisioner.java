package pe.smartcash.cash.workspaces.interfaces.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.iam.domain.model.events.AccountRegisteredEvent;
import pe.smartcash.cash.workspaces.domain.services.WorkspaceCommandService;

/**
 * ACL de entrada: Workspaces escucha el alta de cuenta que emite IAM y crea el módulo
 * "General" del usuario -- el destino por defecto de los gastos que Luki lee de los correos.
 * Vive en {@code interfaces} (no application) por el mismo motivo que el handler homónimo de
 * Profile: es un borde de entrada, análogo a un controller, solo que el "request" es un
 * evento en proceso. El nombre de la clase difiere del de Profile a propósito -- dos beans
 * con el mismo nombre simple colisionan en el escaneo de componentes.
 *
 * <p>{@code @EventListener} síncrono: corre en el mismo hilo y la misma transacción que el
 * sign-up de IAM, así que si el aprovisionamiento del módulo falla, todo el alta hace
 * rollback -- el onboarding queda todo o nada, igual que el registro del perfil.
 */
@Component
class AccountRegisteredWorkspaceProvisioner {

  private final WorkspaceCommandService workspaceCommandService;

  AccountRegisteredWorkspaceProvisioner(WorkspaceCommandService workspaceCommandService) {
    this.workspaceCommandService = workspaceCommandService;
  }

  @EventListener
  void on(AccountRegisteredEvent event) {
    workspaceCommandService.provisionDefaultFor(event.userId().toString());
  }
}
