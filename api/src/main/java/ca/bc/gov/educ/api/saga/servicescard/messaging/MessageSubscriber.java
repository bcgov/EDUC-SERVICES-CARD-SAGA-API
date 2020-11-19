package ca.bc.gov.educ.api.saga.servicescard.messaging;

import ca.bc.gov.educ.api.saga.servicescard.orchestrator.SagaEventHandler;
import ca.bc.gov.educ.api.saga.servicescard.struct.Event;
import ca.bc.gov.educ.api.saga.servicescard.utils.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;


@Component
@Slf4j
public class MessageSubscriber extends MessagePubSub {

  @Getter(PRIVATE)
  private final Map<String, SagaEventHandler> handlers = new HashMap<>();

  @Autowired
  public MessageSubscriber(final Connection con) {
    super.connection = con;
  }


  @SuppressWarnings("java:S2142")
  public void subscribe(String topic, SagaEventHandler eventHandler) {
    handlers.put(topic, eventHandler);
    String queue = topic.replace("_", "-");
    var dispatcher = connection.createDispatcher(onMessage(eventHandler));
    dispatcher.subscribe(topic, queue);
  }

  /**
   * On message message handler.
   *
   * @return the message handler
   */
  private MessageHandler onMessage(SagaEventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received is :: {} ", message);
        try {
          var eventString = new String(message.getData());
          var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.onSagaEvent(event);
          log.debug("Event is :: {}", event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }


}
