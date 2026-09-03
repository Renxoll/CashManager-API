package pe.smartcash.cash.workspaces.domain.model.aggregates;

import java.util.Objects;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;

/**
 * Entidad interna del agregado {@link Workspace} -- nunca se carga ni se guarda por su
 * cuenta, siempre a través del módulo que la contiene. El {@code code} es inmutable (lo
 * referencian las transacciones de ese módulo); el rótulo, el ícono y el estado archivado sí
 * cambian, pero solo vía métodos del agregado raíz.
 */
public final class WorkspaceCategory {

  private final WorkspaceCategoryId id;
  private final String code;
  private String displayName;
  private String icon;
  private int position;
  private boolean archived;

  private WorkspaceCategory(
      WorkspaceCategoryId id, String code, String displayName, String icon, int position, boolean archived) {
    this.id = Objects.requireNonNull(id, "id");
    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("code no puede estar vacío");
    }
    this.code = code;
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
    this.displayName = displayName;
    this.icon = icon;
    this.position = position;
    this.archived = archived;
  }

  static WorkspaceCategory create(WorkspaceCategoryId id, String code, String displayName, String icon, int position) {
    return new WorkspaceCategory(id, code, displayName, icon, position, false);
  }

  public static WorkspaceCategory rehydrate(
      WorkspaceCategoryId id, String code, String displayName, String icon, int position, boolean archived) {
    return new WorkspaceCategory(id, code, displayName, icon, position, archived);
  }

  void rename(String displayName, String icon) {
    if (displayName == null || displayName.isBlank()) {
      throw new IllegalArgumentException("displayName no puede estar vacío");
    }
    this.displayName = displayName;
    if (icon != null && !icon.isBlank()) {
      this.icon = icon;
    }
  }

  void archive() {
    this.archived = true;
  }

  public WorkspaceCategoryId id() {
    return id;
  }

  public String code() {
    return code;
  }

  public String displayName() {
    return displayName;
  }

  public String icon() {
    return icon;
  }

  public int position() {
    return position;
  }

  public boolean archived() {
    return archived;
  }
}
