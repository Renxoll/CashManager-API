package pe.smartcash.cash.transactions.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pe.smartcash.cash.transactions.domain.exception.TransactionExtractionFailedException;
import pe.smartcash.cash.transactions.domain.model.valueobjects.CategoryCode;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionType;

class ExtractedTransactionPayloadTest {

  @Test
  void convertsTheYapeServicePaymentExtraction() {
    var payload = new ExtractedTransactionPayload(new BigDecimal("10.00"), "PEN", "Metropolitano Y Corredores", "TRANSPORTE");

    var result = payload.toResult();

    assertThat(result.money().amount()).isEqualByComparingTo("10.00");
    assertThat(result.money().currency()).isEqualTo("PEN");
    assertThat(result.merchant().name()).isEqualTo("Metropolitano Y Corredores");
    assertThat(result.categoryCode()).isEqualTo(CategoryCode.TRANSPORTE);
    assertThat(result.type()).isEqualTo(TransactionType.EXPENSE);
  }

  @Test
  void roundsAmountsWithMoreThanTwoDecimalsInsteadOfRejectingThem() {
    var payload = new ExtractedTransactionPayload(new BigDecimal("10.005"), "pen", "Metro", "OTROS");

    var result = payload.toResult();

    assertThat(result.money().amount()).isEqualByComparingTo("10.01");
    assertThat(result.money().currency()).isEqualTo("PEN");
  }

  @Test
  void invalidValuesBecomeAnExtractionFailureSoTheFallbackProviderCanRun() {
    assertThatThrownBy(() -> new ExtractedTransactionPayload(new BigDecimal("10.00"), "S/", "Metro", "OTROS").toResult())
        .isInstanceOf(TransactionExtractionFailedException.class);
    assertThatThrownBy(() -> new ExtractedTransactionPayload(new BigDecimal("10.00"), "PEN", "  ", "OTROS").toResult())
        .isInstanceOf(TransactionExtractionFailedException.class);
    assertThatThrownBy(() -> new ExtractedTransactionPayload(null, "PEN", "Metro", "OTROS").toResult())
        .isInstanceOf(TransactionExtractionFailedException.class);
  }

  @Test
  void userPromptTruncatesHugeEmails() {
    String prompt = ExtractionPrompts.userPrompt("x".repeat(50_000));

    assertThat(prompt.length()).isLessThan(ExtractionPrompts.MAX_RAW_TEXT_CHARS + 100);
  }
}
