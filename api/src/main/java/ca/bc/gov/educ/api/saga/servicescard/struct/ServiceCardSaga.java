package ca.bc.gov.educ.api.saga.servicescard.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceCardSaga {
  private static final long serialVersionUID = 1L;
  private String sagaName; // just a place holder until we have api implementation.
}
