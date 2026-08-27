package pe.smartcash.cash.transactions.infrastructure.persistence;

import java.util.List;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.CategoryDescriptor;
import pe.smartcash.cash.transactions.infrastructure.persistence.jpa.repositories.CategoryJpaRepository;

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
    return toDescriptor(entity);
  }

  @Override
  public List<CategoryDescriptor> describeAll() {
    return categoryJpaRepository.findAllByOrderById().stream().map(this::toDescriptor).toList();
  }

  private CategoryDescriptor toDescriptor(CategoryJpaEntity entity) {
    return new CategoryDescriptor(entity.getId(), CategoryCode.fromCode(entity.getCode()), entity.getDisplayName(), entity.getIcon());
  }
}
