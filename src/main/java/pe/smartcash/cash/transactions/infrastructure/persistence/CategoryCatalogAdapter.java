package pe.smartcash.cash.transactions.infrastructure.persistence;

import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.CategoryDescriptor;

@Component
class CategoryCatalogAdapter implements CategoryCatalog {

  private final CategoryJpaRepository categoryJpaRepository;

  CategoryCatalogAdapter(CategoryJpaRepository categoryJpaRepository) {
    this.categoryJpaRepository = categoryJpaRepository;
  }

  @Override
  public CategoryDescriptor describe(CategoryCode code) {
    CategoryJpaEntity entity =
        categoryJpaRepository
            .findByCode(code.name())
            .orElseThrow(() -> new IllegalStateException("Categoría no encontrada en catálogo: " + code));
    return new CategoryDescriptor(entity.getId(), code, entity.getDisplayName(), entity.getIcon());
  }
}
