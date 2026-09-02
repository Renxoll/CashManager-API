package pe.smartcash.cash.workspaces.domain.model.aggregates;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import pe.smartcash.cash.workspaces.domain.exception.DefaultWorkspaceProtectedException;
import pe.smartcash.cash.workspaces.domain.exception.DuplicateWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.exception.LastActiveWorkspaceCategoryException;
import pe.smartcash.cash.workspaces.domain.exception.WorkspaceCategoryNotFoundException;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.CategoryTemplate;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.UserId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceCategoryId;
import pe.smartcash.cash.workspaces.domain.model.valueobjects.WorkspaceId;

/**
 * Aggregate root del bounded context Workspaces: un "módulo" en el que el usuario agrupa
 * gastos aparte de sus gastos personales (p. ej. "Empresa", "Hijo", "Inversiones"), cada uno
 * con su propia lista de categorías. A diferencia de {@code Group} o {@code Transaction}, sí
 * carga su colección hija ({@link WorkspaceCategory}) en memoria: es corta (unas pocas
 * categorías) y toda mutación de una categoría pasa por una invariante del módulo -- que
 * nunca se quede sin categorías activas -- así que no tendría sentido dejarla como agregado
 * independiente.
 *
 * <p>El módulo {@code isDefault} ("General") es el destino de los gastos que Luki lee solo de
 * los correos: se crea en el onboarding, no se puede archivar, pero sí renombrar y
 * personalizar como cualquier otro.
 */
public final class Workspace {

  private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
  private static final String DEFAULT_COLOR = "#8B5CF6";
  private static final String DEFAULT_ICON = "wallet";
  private static final int MAX_NAME = 60;

  private final WorkspaceId id;
  private final UserId ownerId;
  private final boolean isDefault;
  private final Instant createdAt;
  private String name;
  private String colorHex;
  private String icon;
  private Instant archivedAt;
  private final List<WorkspaceCategory> categories = new ArrayList<>();

  private Workspace(
      WorkspaceId id,
      UserId ownerId,
      String name,
      String colorHex,
      String icon,
      boolean isDefault,
      Instant createdAt,
      Instant archivedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
    this.name = requireName(name);
    this.colorHex = normalizeColor(colorHex);
    this.icon = normalizeIcon(icon);
    this.isDefault = isDefault;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.archivedAt = archivedAt;
  }

  /** Módulo "General": destino por defecto de la ingesta automática. Uno solo por usuario. */
  public static Workspace openDefault(WorkspaceId id, UserId ownerId, List<CategoryTemplate> starter, Instant now) {
    return open(id, ownerId, "General", DEFAULT_COLOR, DEFAULT_ICON, true, starter, now);
  }

  /** Módulo custom creado por el usuario -- lo nombra y personaliza a gusto. */
  public static Workspace open(
      WorkspaceId id,
      UserId ownerId,
      String name,
      String colorHex,
      String icon,
      List<CategoryTemplate> starter,
      Instant now) {
    return open(id, ownerId, name, colorHex, icon, false, starter, now);
  }

  private static Workspace open(
      WorkspaceId id,
      UserId ownerId,
      String name,
      String colorHex,
      String icon,
      boolean isDefault,
      List<CategoryTemplate> starter,
      Instant now) {
    Workspace workspace = new Workspace(id, ownerId, name, colorHex, icon, isDefault, now, null);
    int position = 0;
    for (CategoryTemplate template : starter) {
      workspace.categories.add(
          WorkspaceCategory.create(
              WorkspaceCategoryId.newId(),
              template.code().trim().toUpperCase(Locale.ROOT),
              template.displayName(),
              template.icon(),
              position++));
    }
    if (workspace.activeCategories().isEmpty()) {
      throw new IllegalArgumentException("un módulo necesita al menos una categoría");
    }
    return workspace;
  }

  public static Workspace rehydrate(
      WorkspaceId id,
      UserId ownerId,
      String name,
      String colorHex,
      String icon,
      boolean isDefault,
      Instant createdAt,
      Instant archivedAt,
      List<WorkspaceCategory> categories) {
    Workspace workspace = new Workspace(id, ownerId, name, colorHex, icon, isDefault, createdAt, archivedAt);
    workspace.categories.addAll(categories);
    return workspace;
  }

  public void rename(String newName) {
    this.name = requireName(newName);
  }

  public void recustomize(String colorHex, String icon) {
    if (colorHex != null && !colorHex.isBlank()) {
      this.colorHex = normalizeColor(colorHex);
    }
    if (icon != null && !icon.isBlank()) {
      this.icon = normalizeIcon(icon);
    }
  }

  public void archive(Instant now) {
    if (isDefault) {
      throw new DefaultWorkspaceProtectedException(id);
    }
    if (archivedAt == null) {
      this.archivedAt = Objects.requireNonNull(now, "now");
    }
  }

  /**
   * Agrega una categoría nueva. El {@code code} se deriva del rótulo (mayúsculas, sin
   * espacios ni tildes) por el caller y llega ya resuelto acá; el agregado solo garantiza
   * que no choque con otra categoría activa del mismo módulo.
   */
  public WorkspaceCategory addCategory(WorkspaceCategoryId categoryId, String code, String displayName, String icon) {
    String normalized = code.trim().toUpperCase(Locale.ROOT);
    boolean clash = categories.stream().anyMatch(c -> !c.archived() && c.code().equals(normalized));
    if (clash) {
      throw new DuplicateWorkspaceCategoryException(id, normalized);
    }
    int nextPosition = categories.stream().mapToInt(WorkspaceCategory::position).max().orElse(-1) + 1;
    WorkspaceCategory category = WorkspaceCategory.create(categoryId, normalized, displayName, icon, nextPosition);
    categories.add(category);
    return category;
  }

  public void renameCategory(WorkspaceCategoryId categoryId, String displayName, String icon) {
    category(categoryId).rename(displayName, icon);
  }

  public void archiveCategory(WorkspaceCategoryId categoryId) {
    WorkspaceCategory target = category(categoryId);
    if (!target.archived() && activeCategories().size() <= 1) {
      throw new LastActiveWorkspaceCategoryException(id);
    }
    target.archive();
  }

  private WorkspaceCategory category(WorkspaceCategoryId categoryId) {
    return categories.stream()
        .filter(c -> c.id().equals(categoryId))
        .findFirst()
        .orElseThrow(() -> new WorkspaceCategoryNotFoundException(categoryId));
  }

  private static String requireName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("name no puede estar vacío");
    }
    String trimmed = value.trim();
    if (trimmed.length() > MAX_NAME) {
      throw new IllegalArgumentException("name no puede superar " + MAX_NAME + " caracteres");
    }
    return trimmed;
  }

  private static String normalizeColor(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT_COLOR;
    }
    String trimmed = value.trim();
    if (!HEX_COLOR.matcher(trimmed).matches()) {
      throw new IllegalArgumentException("colorHex debe tener formato #RRGGBB: " + value);
    }
    return trimmed.toUpperCase(Locale.ROOT);
  }

  private static String normalizeIcon(String value) {
    if (value == null || value.isBlank()) {
      return DEFAULT_ICON;
    }
    String trimmed = value.trim();
    if (trimmed.length() > 40) {
      throw new IllegalArgumentException("icon no puede superar 40 caracteres");
    }
    return trimmed;
  }

  public WorkspaceId id() {
    return id;
  }

  public UserId ownerId() {
    return ownerId;
  }

  public String name() {
    return name;
  }

  public String colorHex() {
    return colorHex;
  }

  public String icon() {
    return icon;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public boolean archived() {
    return archivedAt != null;
  }

  public Instant archivedAt() {
    return archivedAt;
  }

  public Instant createdAt() {
    return createdAt;
  }

  /** Todas las categorías (activas y archivadas), ordenadas por posición. */
  public List<WorkspaceCategory> categories() {
    return categories.stream().sorted((a, b) -> Integer.compare(a.position(), b.position())).toList();
  }

  public List<WorkspaceCategory> activeCategories() {
    return categories.stream()
        .filter(c -> !c.archived())
        .sorted((a, b) -> Integer.compare(a.position(), b.position()))
        .toList();
  }
}
