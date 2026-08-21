package pe.smartcash.cash.analytics.domain.services;

import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;

/**
 * Contrato de lectura puro: a diferencia de los demás bounded contexts, este no tiene un
 * agregado ni comandos detrás — es un read model que proyecta directamente las tablas de
 * {@code transactions}/{@code categories}, así que no hay invariantes de negocio que proteger
 * acá, solo una consulta rápida para el dashboard.
 */
public interface DashboardQueryService {

  MonthlySummary handle(FindMonthlySummaryQuery query);
}
