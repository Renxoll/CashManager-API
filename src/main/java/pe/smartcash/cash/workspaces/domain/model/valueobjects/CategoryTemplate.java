package pe.smartcash.cash.workspaces.domain.model.valueobjects;

/**
 * Plantilla de categoría con la que se siembra un módulo recién creado. {@code code} es el
 * identificador estable (mayúsculas, sin espacios) y {@code displayName} el rótulo visible.
 */
public record CategoryTemplate(String code, String displayName, String icon) {

  public CategoryTemplate {
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code no puede estar vacío");
    }
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
  }
}
