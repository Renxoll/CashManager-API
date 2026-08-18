package pe.smartcash.cash.transactions.domain.services;

import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;

public interface CategoryCatalog {

  CategoryDescriptor describe(CategoryCode code);
}
