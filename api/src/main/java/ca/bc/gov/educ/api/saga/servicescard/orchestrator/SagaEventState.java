package ca.bc.gov.educ.api.saga.servicescard.orchestrator;

import ca.bc.gov.educ.api.saga.servicescard.constants.EventOutcome;
import ca.bc.gov.educ.api.saga.servicescard.constants.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class SagaEventState<T> {
  private EventOutcome currentEventOutcome;
  private EventType nextEventType;
  private Boolean isCompensating; // does this event out come triggers compensation.
  private SagaStep<T> stepToExecute;
}
