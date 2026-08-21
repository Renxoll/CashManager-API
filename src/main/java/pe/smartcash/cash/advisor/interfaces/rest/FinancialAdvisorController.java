package pe.smartcash.cash.advisor.interfaces.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.advisor.domain.model.queries.AskFinancialAdvisorQuery;
import pe.smartcash.cash.advisor.domain.services.AdvisorReply;
import pe.smartcash.cash.advisor.domain.services.AskFinancialAdvisorUseCase;
import pe.smartcash.cash.advisor.interfaces.rest.resources.ChatMessageRequest;
import pe.smartcash.cash.advisor.interfaces.rest.resources.ChatMessageResponse;
import pe.smartcash.cash.advisor.interfaces.rest.transform.ChatMessageResourceFromEntityAssembler;

/**
 * El {@code userId} sale del principal ya autenticado por {@code BearerTokenAuthenticationFilter}
 * de IAM (mismo criterio que {@code UserProfileController}/{@code DashboardController}): el
 * usuario nunca puede pedir el contexto financiero de otro pasando un id distinto.
 */
@RestController
@RequestMapping("/api/v1/advisor")
class FinancialAdvisorController {

  private final AskFinancialAdvisorUseCase askFinancialAdvisorUseCase;

  FinancialAdvisorController(AskFinancialAdvisorUseCase askFinancialAdvisorUseCase) {
    this.askFinancialAdvisorUseCase = askFinancialAdvisorUseCase;
  }

  @PostMapping("/chat")
  ResponseEntity<ChatMessageResponse> chat(
      @AuthenticationPrincipal String authenticatedUserId, @Valid @RequestBody ChatMessageRequest request) {
    AdvisorReply reply =
        askFinancialAdvisorUseCase.handle(new AskFinancialAdvisorQuery(UUID.fromString(authenticatedUserId), request.message()));
    return ResponseEntity.ok(ChatMessageResourceFromEntityAssembler.toResourceFromEntity(reply));
  }
}
