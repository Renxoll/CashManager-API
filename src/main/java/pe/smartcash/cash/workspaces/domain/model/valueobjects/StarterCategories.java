package pe.smartcash.cash.workspaces.domain.model.valueobjects;

import java.util.List;

/**
 * Set de categorías con el que arranca todo módulo nuevo -- las mismas 8 del catálogo
 * histórico de gastos personales. Es solo un punto de partida: apenas creado el módulo, el
 * usuario renombra, agrega o archiva las que quiera sin afectar a ningún otro módulo. La
 * migración V14 siembra estas mismas 8 para el módulo "General" de los usuarios que ya
 * existían (copiándolas de la tabla {@code categories}); esta constante mantiene el mismo
 * contenido para los módulos que se creen de acá en adelante.
 */
public final class StarterCategories {

  private StarterCategories() {}

  public static final List<CategoryTemplate> DEFAULT =
      List.of(
          new CategoryTemplate("COMIDA", "Comida", "utensils"),
          new CategoryTemplate("TRANSPORTE", "Transporte", "car"),
          new CategoryTemplate("ENTRETENIMIENTO", "Entretenimiento", "film"),
          new CategoryTemplate("SALUD", "Salud", "heart-pulse"),
          new CategoryTemplate("COMPRAS", "Compras", "shopping-bag"),
          new CategoryTemplate("SERVICIOS", "Servicios", "receipt"),
          new CategoryTemplate("EDUCACION", "Educación", "book"),
          new CategoryTemplate("OTROS", "Otros", "circle-help"));
}
