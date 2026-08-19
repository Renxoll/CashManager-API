package pe.smartcash.cash.analytics.interfaces.rest;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.DashboardQueryService;
import pe.smartcash.cash.analytics.interfaces.rest.resources.MonthlySummaryResponse;
import pe.smartcash.cash.analytics.interfaces.rest.transform.MonthlySummaryResourceFromEntityAssembler;

/**
 * El {@code userId} sale del principal ya autenticado por {@code BearerTokenAuthenticationFilter}
 * de IAM (ver el mismo criterio en {@code UserProfileController}), nunca de un path/query
 * param: así un cliente no puede pedir el dashboard financiero de otro usuario cambiando un id.
 */
@RestController
@RequestMapping("/api/v1/analytics")
class DashboardController {

  private final DashboardQueryService dashboardQueryService;

  DashboardController(DashboardQueryService dashboardQueryService) {
    this.dashboardQueryService = dashboardQueryService;
  }

  @GetMapping("/monthly-summary")
  ResponseEntity<MonthlySummaryResponse> monthlySummary(@AuthenticationPrincipal String authenticatedUserId) {
    var summary = dashboardQueryService.handle(new FindMonthlySummaryQuery(UUID.fromString(authenticatedUserId)));
    return ResponseEntity.ok(MonthlySummaryResourceFromEntityAssembler.toResourceFromEntity(summary));
  }
}
