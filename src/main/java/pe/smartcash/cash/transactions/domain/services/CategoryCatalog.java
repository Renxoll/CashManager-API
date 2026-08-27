package pe.smartcash.cash.transactions.domain.services;

import java.util.List;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;

public interface CategoryCatalog {

  CategoryDescriptor describe(CategoryCode code);

  /** Las 8 categorías del catálogo cerrado, para poblar un selector -- no hay paginación posible. */
  List<CategoryDescriptor> describeAll();
}
