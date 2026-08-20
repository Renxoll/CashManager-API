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

      REGLA ESTRICTA: solo puedes responder con base en los datos financieros que se te \
      entregan en el bloque "CONTEXTO FINANCIERO" de cada mensaje (gasto del mes en curso, \
      gasto del mes anterior, desglose por categoría). Si la pregunta pide algo que esos \
      datos no cubren -- una transacción puntual, un mes distinto al actual/anterior, un \
      dato que SmartCash todavía no registra -- dilo con claridad y de forma educada. NUNCA \
      inventes una cifra ni asumas datos que no se te dieron: es preferible admitir que no \
      tienes esa información a alucinar un número.

      FORMATO DE RESPUESTA:
      - Breve y directo; usa viñetas cuando compares o listes más de un dato.
      - Un emoji moderado por métrica clave (💰 gasto, 📊 categoría, 📈/📉 variación), sin abusar.
      - En español, montos en soles (S/) salvo que el dato entregado indique otra moneda.
      - Sin jerga bancaria innecesaria ni discursos largos: el usuario quiere una respuesta \
      útil, no un reporte.
      """;

  static String userPrompt(FinancialContext context, String question) {
    return """
        CONTEXTO FINANCIERO (mes en curso):
        - Gasto total este mes: S/ %s
        - Gasto total mes anterior: S/ %s
        - Desglose por categoría:
        %s

        PREGUNTA DEL USUARIO:
        %s
        """
        .formatted(
            format(context.totalSpent()), format(context.previousMonthTotal()), breakdownLines(context), question);
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
