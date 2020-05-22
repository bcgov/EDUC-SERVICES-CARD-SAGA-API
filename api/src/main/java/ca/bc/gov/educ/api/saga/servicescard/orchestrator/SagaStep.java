package ca.bc.gov.educ.api.saga.servicescard.orchestrator;


import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSaga;
import ca.bc.gov.educ.api.saga.servicescard.struct.Event;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface SagaStep<T> {
  /**
   * This method is a convenient way to execute next steps in the saga workflow.
   *
   * @param event    the event which was received in the message.
   * @param saga     the model object.
   * @param sagaData the initial payload for the saga.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  void apply(Event event, ServicesCardSaga saga, T sagaData) throws InterruptedException, TimeoutException, IOException;
}
