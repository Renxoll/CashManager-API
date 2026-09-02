package pe.smartcash.cash.transactions.domain.services;

/** Vista mínima de una categoría de módulo custom, resuelta vía {@link WorkspaceDirectory}
 * para armar los read-models de transacciones. */
public record WorkspaceCategoryView(String code, String displayName, String icon) {}
