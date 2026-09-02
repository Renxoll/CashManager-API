package pe.smartcash.cash.workspaces.domain.exception;

import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;

public class WorkspaceCategoryNotFoundException extends RuntimeException {

  public WorkspaceCategoryNotFoundException(WorkspaceCategoryId categoryId) {
    super("Categoría de módulo no encontrada: " + categoryId.value());
  }
}
