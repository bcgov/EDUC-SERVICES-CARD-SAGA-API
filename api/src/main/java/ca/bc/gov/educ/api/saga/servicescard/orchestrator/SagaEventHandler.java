package ca.bc.gov.educ.api.saga.servicescard.orchestrator;


import ca.bc.gov.educ.api.saga.servicescard.struct.Event;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface SagaEventHandler {
  /**
   * this method is called when there is a message on the topic this api subscribing to.
   *
   * @param event The Event object parsed from Event JSON string.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  void onSagaEvent(Event event) throws InterruptedException, IOException, TimeoutException;
}
