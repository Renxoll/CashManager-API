package pe.smartcash.cash.advisor.interfaces.rest.transform;

import pe.smartcash.cash.advisor.domain.services.AdvisorReply;
import pe.smartcash.cash.advisor.interfaces.rest.resources.ChatMessageResponse;

public final class ChatMessageResourceFromEntityAssembler {

  private ChatMessageResourceFromEntityAssembler() {}

  public static ChatMessageResponse toResourceFromEntity(AdvisorReply reply) {
    return new ChatMessageResponse(reply.reply(), reply.timestamp());
  }
}
