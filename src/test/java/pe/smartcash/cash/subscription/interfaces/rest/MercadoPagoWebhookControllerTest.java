package pe.smartcash.cash.subscription.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.smartcash.cash.subscription.domain.model.commands.HandlePaymentEventCommand;
import pe.smartcash.cash.subscription.domain.services.SubscriptionCommandService;

class MercadoPagoWebhookControllerTest {

  private static final String SECRET = "clave-secreta-del-panel";

  private SubscriptionCommandService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(SubscriptionCommandService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(service, SECRET)).build();
  }

  @Test
  void aSignedNotificationIsHandledWithTypeAndDataId() throws Exception {
    mockMvc.perform(notification("2C93808", "subscription_preapproval", sign("2c93808", "req-1", "1700000000"), "req-1"))
        .andExpect(status().isOk());

    ArgumentCaptor<HandlePaymentEventCommand> captor = ArgumentCaptor.forClass(HandlePaymentEventCommand.class);
    verify(service).handle(captor.capture());
    assertThat(captor.getValue().eventId()).isEqualTo("subscription_preapproval:2C93808");
  }

  @Test
  void aWrongSignatureIsForbiddenAndNothingIsHandled() throws Exception {
    mockMvc.perform(notification("123", "subscription_preapproval", "ts=1700000000,v1=deadbeef", "req-1")).andExpect(status().isForbidden());
    verify(service, never()).handle(any(HandlePaymentEventCommand.class));
  }

  @Test
  void aMissingSignatureIsForbidden() throws Exception {
    mockMvc.perform(post("/api/v1/subscriptions/mercadopago-webhook").param("data.id", "123").param("type", "subscription_preapproval"))
        .andExpect(status().isForbidden());
  }

  @Test
  void aSignatureForAnotherDataIdIsForbidden() throws Exception {
    mockMvc.perform(notification("999", "subscription_preapproval", sign("123", "req-1", "1700000000"), "req-1")).andExpect(status().isForbidden());
  }

  @Test
  void withoutAConfiguredSecretEverythingIsForbidden() throws Exception {
    MockMvc noSecret = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController(service, "")).build();
    noSecret.perform(notification("123", "subscription_preapproval", sign("123", "req-1", "1700000000"), "req-1")).andExpect(status().isForbidden());
  }

  @Test
  void theTypeCanComeFromTheBody() throws Exception {
    mockMvc.perform(post("/api/v1/subscriptions/mercadopago-webhook")
            .param("data.id", "123")
            .header("x-signature", sign("123", "req-1", "1700000000"))
            .header("x-request-id", "req-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"type\":\"subscription_authorized_payment\"}"))
        .andExpect(status().isOk());

    ArgumentCaptor<HandlePaymentEventCommand> captor = ArgumentCaptor.forClass(HandlePaymentEventCommand.class);
    verify(service).handle(captor.capture());
    assertThat(captor.getValue().eventId()).isEqualTo("subscription_authorized_payment:123");
  }

  @Test
  void aSignedNotificationWithoutTypeIsABadRequest() throws Exception {
    mockMvc.perform(post("/api/v1/subscriptions/mercadopago-webhook")
            .param("data.id", "123")
            .header("x-signature", sign("123", "req-1", "1700000000"))
            .header("x-request-id", "req-1"))
        .andExpect(status().isBadRequest());
  }

  private static org.springframework.test.web.servlet.RequestBuilder notification(String dataId, String type, String signature, String requestId) {
    return post("/api/v1/subscriptions/mercadopago-webhook")
        .param("data.id", dataId)
        .param("type", type)
        .header("x-signature", signature)
        .header("x-request-id", requestId);
  }

  private static String sign(String signedDataId, String requestId, String ts) {
    String hash = MercadoPagoWebhookSignature.hmacSha256Hex(SECRET, "id:" + signedDataId + ";request-id:" + requestId + ";ts:" + ts + ";");
    return "ts=" + ts + ",v1=" + hash;
  }
}
