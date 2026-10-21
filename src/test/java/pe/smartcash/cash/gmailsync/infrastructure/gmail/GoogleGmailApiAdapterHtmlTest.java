package pe.smartcash.cash.gmailsync.infrastructure.gmail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El texto que llega al LLM viene de {@code stripHtml}: tiene que quedar solo el contenido legible. */
class GoogleGmailApiAdapterHtmlTest {

  private static final String YAPE_HTML =
      """
      <html><head><title>Yape</title>
      <style type="text/css">.btn { color: #fff; } @media (max-width:600px) { .x { width: 100%; } }</style>
      <script>var a = "S/ 999.99";</script></head>
      <body><!-- S/ 0.01 comentario -->
      <p>Hola RENZO,</p><p>&iexcl;Tu servicio fue yapeado con &eacute;xito!</p>
      <table><tr><td>Monto total</td><td>S/&nbsp;10.00</td></tr>
      <tr><td>Empresa:</td><td>Metropolitano y Corredores</td></tr></table>
      <p>Cr&#233;dito &amp; m&#xE1;s</p></body></html>
      """;

  @Test
  void dropsHeadStyleScriptAndCommentsWithTheirContent() {
    String text = GoogleGmailApiAdapter.stripHtml(YAPE_HTML);

    assertThat(text).doesNotContain("color: #fff", "max-width", "999.99", "0.01", "<", ">");
  }

  @Test
  void keepsTheAmountAndTheMerchantReadable() {
    String text = GoogleGmailApiAdapter.stripHtml(YAPE_HTML);

    assertThat(text).contains("Monto total S/ 10.00").contains("Empresa: Metropolitano y Corredores");
  }

  @Test
  void decodesNamedAndNumericEntities() {
    String text = GoogleGmailApiAdapter.stripHtml(YAPE_HTML);

    assertThat(text).contains("¡Tu servicio fue yapeado con éxito!").contains("Crédito & más");
  }
}
