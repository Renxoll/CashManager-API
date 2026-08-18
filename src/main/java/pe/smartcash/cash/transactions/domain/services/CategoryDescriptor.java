package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;

/** Vista de solo lectura del catálogo (id de persistencia + nombre visible + ícono). */
public record CategoryDescriptor(Long id, CategoryCode code, String displayName, String icon) {}
