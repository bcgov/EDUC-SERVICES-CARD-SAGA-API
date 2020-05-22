package ca.bc.gov.educ.api.saga.servicescard.repository;

import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSaga;
import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSagaEventState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServicesCardSagaEventStatesRepository extends CrudRepository<ServicesCardSagaEventState, UUID> {
  List<ServicesCardSagaEventState> findByServicesCardSaga(ServicesCardSaga servicesCardSaga);

  Optional<ServicesCardSagaEventState>
  findByServicesCardSagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(ServicesCardSaga servicesCardSaga, String eventOutcome, String eventState, int stepNumber);
}
