package pe.smartcash.cash.transactions.domain.services;

/**
 * Categoría de una transacción ya resuelta a texto, sin importar si vino del catálogo
 * cerrado del módulo General ({@code CategoryCode}) o de una categoría propia de un módulo
 * custom ({@code workspace_categories}). El {@code code} es informativo para el cliente;
 * para reasignar categoría se manda de vuelta ese mismo code.
 */
public record ResolvedCategory(String code, String displayName, String icon) {}
