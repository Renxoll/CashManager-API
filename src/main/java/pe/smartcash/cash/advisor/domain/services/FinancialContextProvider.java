package pe.smartcash.cash.advisor.domain.services;

import java.util.UUID;

/**
 * Puerto (Anti-Corruption Layer) hacia el bounded context Analytics — mismo rol que {@code
 * transactions.domain.services.UserDirectory} hacia Profile: es el único punto donde este
 * contexto conoce que Analytics existe, y lo hace solo a través de su API pública de dominio
 * ({@code DashboardQueryService}), nunca importando {@code analytics.domain.model.*}.
 */
public interface FinancialContextProvider {

  FinancialContext currentMonthContext(UUID userId);
}
