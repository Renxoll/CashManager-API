package pe.smartcash.cash.workspaces.application.internal.commandservices;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Deriva el {@code code} estable de una categoría a partir del rótulo que escribe el usuario
 * ("Nómina de empleados" -> "NOMINA_DE_EMPLEADOS"): quita tildes, pasa a mayúsculas y
 * reemplaza todo lo que no sea A-Z/0-9 por guion bajo. Si el resultado choca con un code ya
 * usado en ese módulo, le añade un sufijo numérico.
 */
final class CategoryCodeFactory {

  private CategoryCodeFactory() {}

  static String from(String displayName, Set<String> existingCodes) {
    String base = slugify(displayName);
    if (base.isBlank()) {
      base = "CATEGORIA";
    }
    if (base.length() > 40) {
      base = base.substring(0, 40);
    }
    if (!existingCodes.contains(base)) {
      return base;
    }
    for (int suffix = 2; suffix < 1000; suffix++) {
      String candidate = trimTo(base, 40 - ("_" + suffix).length()) + "_" + suffix;
      if (!existingCodes.contains(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("No se pudo generar un code único para la categoría: " + displayName);
  }

  private static String slugify(String value) {
    if (value == null) {
      return "";
    }
    String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    return noAccents
        .toUpperCase(Locale.ROOT)
        .replaceAll("[^A-Z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
  }

  private static String trimTo(String value, int max) {
    return value.length() <= max ? value : value.substring(0, Math.max(0, max));
  }
}
