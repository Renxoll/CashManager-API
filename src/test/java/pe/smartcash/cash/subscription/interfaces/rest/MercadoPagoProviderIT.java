package pe.smartcash.cash.subscription.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.smartcash.cash.RedisTestConfiguration;
import pe.smartcash.cash.subscription.domain.services.SubscriptionPaymentGateway;

/** Con {@code app.payments.provider=mercadopago} el contexto arma SOLO el adaptador y el webhook de Mercado Pago. */
@SpringBootTest(
    properties = {
      "app.payments.provider=mercadopago",
      "app.mercadopago.access-token=TEST-it",
      "app.mercadopago.webhook-secret=secreto-it"
    })
@AutoConfigureMockMvc
@Testcontainers
@Import(RedisTestConfiguration.class)
class MercadoPagoProviderIT {

  @Container
  @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

  @Autowired private ApplicationContext context;
  @Autowired private MockMvc mockMvc;
  @Autowired private SubscriptionPaymentGateway gateway;

  @Test
  void onlyTheMercadoPagoAdapterIsWired() {
    assertThat(gateway.getClass().getSimpleName()).isEqualTo("MercadoPagoPaymentGatewayAdapter");
    assertThat(context.containsBean("mercadoPagoRestClient")).isTrue();
  }

  @Test
  void theWebhookIsPublicButRejectsAnUnsignedCall() throws Exception {
    mockMvc.perform(post("/api/v1/subscriptions/mercadopago-webhook").param("data.id", "1").param("type", "subscription_preapproval"))
        .andExpect(status().isForbidden());
  }

  @Test
  void theCulqiAdapterAndWebhookAreNotWired() {
    assertThat(context.containsBean("culqiPaymentGatewayAdapter")).isFalse();
    assertThat(context.containsBean("culqiWebhookController")).isFalse();
  }
}
