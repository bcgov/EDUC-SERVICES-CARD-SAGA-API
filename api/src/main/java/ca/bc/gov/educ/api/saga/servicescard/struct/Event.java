package ca.bc.gov.educ.api.saga.servicescard.struct;

import ca.bc.gov.educ.api.saga.servicescard.constants.EventOutcome;
import ca.bc.gov.educ.api.saga.servicescard.constants.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
public class Event {
  private EventType eventType;
  private EventOutcome eventOutcome;
  private UUID sagaId;
  private String replyTo;
  private String eventPayload; // json string
}
