package pe.smartcash.cash.subscription.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class CulqiEventDataTest {

  private final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  void theSubscriptionItselfAsData() {
    assertThat(find("{\"object\":\"subscription\",\"id\":\"sxn_live_abc\",\"status\":3}")).isEqualTo("sxn_live_abc");
  }

  @Test
  void aChargeThatPointsToItsSubscriptionByField() {
    assertThat(find("{\"object\":\"charge\",\"id\":\"chr_live_1\",\"subscription_id\":\"sxn_live_abc\"}")).isEqualTo("sxn_live_abc");
  }

  @Test
  void aNestedSubscriptionObject() {
    assertThat(find("{\"object\":\"charge\",\"id\":\"chr_live_1\",\"subscription\":{\"id\":\"sxn_live_abc\"}}")).isEqualTo("sxn_live_abc");
  }

  @Test
  void anIdBuriedDeeperIsFoundByItsPrefix() {
    assertThat(find("{\"object\":\"charge\",\"periods\":[{\"charges\":{\"ref\":\"sxn_test_xyz\"}}]}")).isEqualTo("sxn_test_xyz");
  }

  @Test
  void idsOfOtherCulqiObjectsAreNeverMistakenForASubscription() {
    assertThat(find("{\"object\":\"charge\",\"id\":\"chr_live_1\",\"card_id\":\"crd_live_1\",\"customer_id\":\"cus_live_1\"}")).isNull();
  }

  @Test
  void missingOrNullDataYieldsNothing() {
    assertThat(CulqiEventData.findSubscriptionId(mapper.missingNode())).isNull();
    assertThat(find("null")).isNull();
  }

  private String find(String json) {
    return CulqiEventData.findSubscriptionId(mapper.readTree(json));
  }
}
