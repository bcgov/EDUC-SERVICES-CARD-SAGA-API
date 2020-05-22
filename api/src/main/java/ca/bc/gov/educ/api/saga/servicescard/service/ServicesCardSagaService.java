package ca.bc.gov.educ.api.saga.servicescard.service;

import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSaga;
import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSagaEventState;
import ca.bc.gov.educ.api.saga.servicescard.repository.ServicesCardSagaEventStatesRepository;
import ca.bc.gov.educ.api.saga.servicescard.repository.ServicesCardSagaRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.api.saga.servicescard.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.saga.servicescard.constants.SagaStatusEnum.STARTED;
import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class ServicesCardSagaService {
  @Getter(AccessLevel.PRIVATE)
  private final ServicesCardSagaRepository sagaRepository;
  @Getter(PRIVATE)
  private final ServicesCardSagaEventStatesRepository sagaEventStatesRepository;
  private static final String SERVICES_CARD_SAGA_API = "SERVICES_CARD_SAGA_API";

  @Autowired
  public ServicesCardSagaService(final ServicesCardSagaRepository sagaRepository, ServicesCardSagaEventStatesRepository sagaEventStatesRepository) {
    this.sagaRepository = sagaRepository;
    this.sagaEventStatesRepository = sagaEventStatesRepository;
  }



  /**
   * no need to do a get here as it is an attached entity
   * first find the child record, if exist do not add. this scenario may occur in replay process,
   * so dont remove this check. removing this check will lead to duplicate records in the child table.
   *
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  public void updateAttachedSagaWithEvents(ServicesCardSaga saga, ServicesCardSagaEventState sagaEventState) {
    saga.setUpdateDate(LocalDateTime.now());
    getSagaRepository().save(saga);
    val result = getSagaEventStatesRepository()
            .findByServicesCardSagaAndSagaEventOutcomeAndSagaEventStateAndSagaStepNumber(saga, sagaEventState.getSagaEventOutcome(), sagaEventState.getSagaEventState(), sagaEventState.getSagaStepNumber() - 1); //check if the previous step was same and had same outcome, and it is due to replay.
    if (!result.isPresent()) {
      getSagaEventStatesRepository().save(sagaEventState);
    }
  }

  public Optional<ServicesCardSaga> findSagaById(UUID sagaId) {
    return getSagaRepository().findById(sagaId);
  }

  public List<ServicesCardSagaEventState> findAllSagaStates(ServicesCardSaga servicesCardSaga) {
    return getSagaEventStatesRepository().findByServicesCardSaga(servicesCardSaga);
  }

  // this needs to be committed to db before next steps are taken.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 2000))
  public ServicesCardSaga updateAttachedSaga(ServicesCardSaga servicesCardSaga) {
    return getSagaRepository().save(servicesCardSaga);
  }

  private ServicesCardSaga getServicesCardSaga(String payload, String sagaName) {
    return ServicesCardSaga
            .builder()
            .payload(payload)
            .sagaName(sagaName)
            .status(STARTED.toString())
            .sagaState(INITIATED.toString())
            .sagaCompensated(false)
            .createDate(LocalDateTime.now())
            .createUser(SERVICES_CARD_SAGA_API)
            .updateUser(SERVICES_CARD_SAGA_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

}
