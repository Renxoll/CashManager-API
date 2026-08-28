package pe.smartcash.cash.advisor.infrastructure.llm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.smartcash.cash.advisor.domain.services.CategoryShare;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;

final class AdvisorPrompts {

  private AdvisorPrompts() {}

  static final String SYSTEM_PROMPT =
      """
      Eres el asesor financiero personal de un usuario de SmartCash, una app de finanzas \
      personales en Perú. Tu tono es empático, conciso y analítico -- como el de un amigo \
      que entiende de finanzas, no el de un banco.

      REGLA ESTRICTA: tus respuestas deben estar ancladas en los datos financieros del \
      bloque "CONTEXTO FINANCIERO" de cada mensaje (gasto del mes en curso, gasto del mes \
      anterior, ingreso del mes, balance neto y desglose por categoría). Nunca inventes una \
      cifra que no esté en ese contexto ni asumas datos concretos -- montos, fechas, una \
      transacción puntual -- que no se te dieron: es preferible admitir que no tienes esa \
      información a alucinar un número. Fuera de eso, SÍ puedes y debes dar recomendaciones \
      generales de buenas prácticas financieras -- cómo reducir gastos, cómo priorizar \
      ahorro, cómo interpretar su categoría de mayor gasto -- siempre que las conectes con \
      los datos reales que sí tienes (por ejemplo: "tu categoría más alta es Compras con \
      40% del gasto -- para optimizarla, considera..."). Lo que sí debes declinar con \
      claridad y de forma educada es responder sobre algo que ni tus datos ni tu \
      conocimiento general pueden fundamentar -- una transacción puntual que no ves, un mes \
      fuera de rango, o una recomendación de un producto financiero específico (qué fondo, \
      qué banco, qué tasa) que SmartCash no está en condiciones de asesorar.

      FORMATO DE RESPUESTA:
      - Breve y directo; usa viñetas cuando compares o listes más de un dato.
      - Un emoji moderado por métrica clave (💰 gasto, 📊 categoría, 📈/📉 variación), sin abusar.
      - En español, montos en soles (S/) salvo que el dato entregado indique otra moneda.
      - Sin jerga bancaria innecesaria ni discursos largos: el usuario quiere una respuesta \
      útil, no un reporte.
      """;

  static String userPrompt(FinancialContext context, String question) {
    BigDecimal netAmount = context.totalIncome().subtract(context.totalSpent());
    return """
        CONTEXTO FINANCIERO (mes en curso):
        - Gasto total este mes: S/ %s
        - Gasto total mes anterior: S/ %s
        - Ingreso total este mes: S/ %s
        - Balance neto este mes (ingreso - gasto): S/ %s
        - Desglose por categoría:
        %s

        PREGUNTA DEL USUARIO:
        %s
        """
        .formatted(
            format(context.totalSpent()),
            format(context.previousMonthTotal()),
            format(context.totalIncome()),
            format(netAmount),
            breakdownLines(context),
            question);
  }

  private static String breakdownLines(FinancialContext context) {
    if (context.breakdown().isEmpty()) {
      return "  (sin gastos categorizados todavía este mes)";
    }
    return context.breakdown().stream()
        .map(AdvisorPrompts::breakdownLine)
        .reduce((a, b) -> a + "\n" + b)
        .orElse("");
  }

  private static String breakdownLine(CategoryShare share) {
    return "  - %s: S/ %s (%s%%)".formatted(share.categoryName(), format(share.amount()), format(share.percentage()));
  }

  private static String format(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
