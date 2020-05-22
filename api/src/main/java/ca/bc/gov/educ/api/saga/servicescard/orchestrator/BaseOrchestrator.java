package ca.bc.gov.educ.api.saga.servicescard.orchestrator;

import ca.bc.gov.educ.api.saga.servicescard.constants.EventOutcome;
import ca.bc.gov.educ.api.saga.servicescard.constants.EventType;
import ca.bc.gov.educ.api.saga.servicescard.messaging.MessagePublisher;
import ca.bc.gov.educ.api.saga.servicescard.messaging.MessageSubscriber;
import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSaga;
import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSagaEventState;
import ca.bc.gov.educ.api.saga.servicescard.poll.EventTaskScheduler;
import ca.bc.gov.educ.api.saga.servicescard.service.ServicesCardSagaService;
import ca.bc.gov.educ.api.saga.servicescard.struct.Event;
import ca.bc.gov.educ.api.saga.servicescard.utils.JsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.api.saga.servicescard.constants.EventOutcome.INITIATE_SUCCESS;
import static ca.bc.gov.educ.api.saga.servicescard.constants.EventType.INITIATED;
import static ca.bc.gov.educ.api.saga.servicescard.constants.SagaStatusEnum.COMPLETED;
import static lombok.AccessLevel.PROTECTED;

/**
 * Generic base class implementation for education, to implement Saga Orchestrator pattern.
 *
 * @param <T>
 * @author om
 */
@Slf4j
public abstract class BaseOrchestrator<T> {

  protected static final String SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT = "system is going to execute next event :: {} for current event {}";
  @Getter(PROTECTED)
  private final ServicesCardSagaService sagaService;
  @Getter(PROTECTED)
  private final MessagePublisher messagePublisher;
  protected final Class<T> clazz;
  @Getter(PROTECTED)
  private final String sagaName;
  @Getter(PROTECTED)
  private final String topicToSubscribe;
  private final Map<EventType, List<SagaEventState<T>>> nextStepsToExecute = new LinkedHashMap<>();


  public BaseOrchestrator(ServicesCardSagaService sagaService, MessagePublisher messagePublisher, MessageSubscriber messageSubscriber, EventTaskScheduler taskScheduler, Class<T> clazz, String sagaName, String topicToSubscribe) {
    this.sagaService = sagaService;
    this.messagePublisher = messagePublisher;
    this.clazz = clazz;
    this.sagaName = sagaName;
    this.topicToSubscribe = topicToSubscribe;
    messageSubscriber.subscribe(topicToSubscribe, this::executeSagaEvent);
    taskScheduler.registerSagaOrchestrator(sagaName, this);
    populateStepsToExecuteMap();
  }

  private List<SagaEventState<T>> createSingleCollectionEventState(EventOutcome eventOutcome, EventType nextEventType, SagaStep<T> stepToExecute) {
    List<SagaEventState<T>> eventStates = new ArrayList<>();
    eventStates.add(buildSagaEventState(eventOutcome, nextEventType, stepToExecute));
    return eventStates;
  }

  private SagaEventState<T> buildSagaEventState(EventOutcome eventOutcome, EventType nextEventType, SagaStep<T> stepToExecute) {
    return SagaEventState.<T>builder().currentEventOutcome(eventOutcome).isCompensating(false).nextEventType(nextEventType).stepToExecute(stepToExecute).build();
  }

  private BaseOrchestrator<T> registerStepToExecute(EventType initEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    if (this.nextStepsToExecute.containsKey(initEvent)) {
      List<SagaEventState<T>> states = this.nextStepsToExecute.get(initEvent);
      states.add(buildSagaEventState(outcome, nextEvent, stepToExecute));
    } else {
      this.nextStepsToExecute.put(initEvent, createSingleCollectionEventState(outcome, nextEvent, stepToExecute));
    }
    return this;
  }

  /**
   * @param currentEvent  the event that has occurred.
   * @param outcome       outcome of the event.
   * @param nextEvent     next event that will occur.
   * @param stepToExecute which method to execute for the next event. it is a lambda function.
   * @return {@link BaseOrchestrator}
   */
  protected BaseOrchestrator<T> step(EventType currentEvent, EventOutcome outcome, EventType nextEvent, SagaStep<T> stepToExecute) {
    return registerStepToExecute(currentEvent, outcome, nextEvent, stepToExecute);
  }

  /**
   * this is a simple and convenient method to trigger builder pattern in the child classes.
   *
   * @return {@link BaseOrchestrator}
   */
  protected BaseOrchestrator<T> stepBuilder() {
    return this;
  }


  /**
   * this method will check if the event is not already processed. this could happen in SAGAs due to duplicate messages.
   * Application should be able to handle this.
   *
   * @param currentEventType current event.
   * @param servicesCardSaga   the model object.
   * @param eventTypes       event types stored in the hashmap
   * @return true or false based on whether the current event with outcome received from the queue is already processed or not.
   */
  protected boolean isNotProcessedEvent(EventType currentEventType, ServicesCardSaga servicesCardSaga, Set<EventType> eventTypes) {
    EventType eventTypeInDB = EventType.valueOf(servicesCardSaga.getSagaState());
    List<EventType> events = new LinkedList<>(eventTypes);
    int dbEventIndex = events.indexOf(eventTypeInDB);
    int currentEventIndex = events.indexOf(currentEventType);
    return currentEventIndex >= dbEventIndex;
  }

  /**
   * creates the ServicesCardSagaEventState object
   *
   * @param servicesCardSaga the payload.
   * @param eventType      event type
   * @param eventOutcome   outcome
   * @param eventPayload   payload.
   * @return {@link ServicesCardSagaEventState}
   */
  protected ServicesCardSagaEventState createEventState(@NotNull ServicesCardSaga servicesCardSaga, @NotNull EventType eventType, @NotNull EventOutcome eventOutcome, String eventPayload) {
    return ServicesCardSagaEventState.builder()
            .createDate(LocalDateTime.now())
            .createUser(sagaName)
            .updateDate(LocalDateTime.now())
            .updateUser(sagaName)
            .servicesCardSaga(servicesCardSaga)
            .sagaEventOutcome(eventOutcome.toString())
            .sagaEventState(eventType.toString())
            .sagaStepNumber(calculateStep(servicesCardSaga))
            .sagaEventResponse(eventPayload == null ? "" : eventPayload)
            .build();
  }

  /**
   * This method updates the DB and marks the process as complete.
   *
   * @param event          the current event.
   * @param servicesCardSaga the saga model object.
   * @param sagaData       the payload string as object.
   */
  protected void markSagaComplete(Event event, ServicesCardSaga servicesCardSaga, T sagaData) {
    log.trace(sagaData.toString());
    ServicesCardSagaEventState eventStates = createEventState(servicesCardSaga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    servicesCardSaga.setSagaState(COMPLETED.toString());
    servicesCardSaga.setStatus(COMPLETED.toString());
    servicesCardSaga.setUpdateDate(LocalDateTime.now());
    getSagaService().updateAttachedSagaWithEvents(servicesCardSaga, eventStates);
  }

  /**
   * calculate step number
   *
   * @param servicesCardSaga the model object.
   * @return step number that was calculated.
   */
  private int calculateStep(ServicesCardSaga servicesCardSaga) {
    val sagaStates = getSagaService().findAllSagaStates(servicesCardSaga);
    return (sagaStates.size() + 1);
  }

  /**
   * convenient method to post message to topic, to be used by child classes.
   *
   * @param topicName topic name where the message will be posted.
   * @param nextEvent the next event object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void postMessageToTopic(String topicName, Event nextEvent) throws InterruptedException, IOException, TimeoutException {
    getMessagePublisher().dispatchMessage(topicName, JsonUtil.getJsonStringFromObject(nextEvent).getBytes());
  }

  /**
   * it finds the last event that was processed successfully for this saga.
   *
   * @param eventStates event states corresponding to the Saga.
   * @return {@link ServicesCardSagaEventState} if found else null.
   */
  protected Optional<ServicesCardSagaEventState> findTheLastEventOccurred(List<ServicesCardSagaEventState> eventStates) {
    int step = eventStates.stream().map(ServicesCardSagaEventState::getSagaStepNumber).mapToInt(x -> x).max().orElse(0);
    return eventStates.stream().filter(element -> element.getSagaStepNumber() == step).findFirst();
  }

  /**
   * this method is called from the cron job , which will replay the saga process based on its current state.
   *
   * @param servicesCardSaga the model object.
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  @Async
  @Transactional
  public void replaySaga(ServicesCardSaga servicesCardSaga) throws IOException, InterruptedException, TimeoutException {
    List<ServicesCardSagaEventState> eventStates = getSagaService().findAllSagaStates(servicesCardSaga);
    T t = JsonUtil.getJsonObjectFromString(clazz, servicesCardSaga.getPayload());
    if (eventStates.isEmpty()) { //process did not start last time, lets start from beginning.
      replayFromBeginning(servicesCardSaga, t);
    } else {
      replayFromLastEvent(servicesCardSaga, eventStates, t);
    }
  }

  /**
   * This method will restart the saga process from where it was left the last time. which could occur due to various reasons
   *
   * @param servicesCardSaga the model object.
   * @param eventStates    the event states corresponding to the saga
   * @param t              the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void replayFromLastEvent(ServicesCardSaga servicesCardSaga, List<ServicesCardSagaEventState> eventStates, T t) throws InterruptedException, TimeoutException, IOException {
    val servicesCardSagaEventStateOptional = findTheLastEventOccurred(eventStates);
    if (servicesCardSagaEventStateOptional.isPresent()) {
      val servicesCardSagaEventState = servicesCardSagaEventStateOptional.get();
      log.trace(servicesCardSagaEventStateOptional.toString());
      EventType currentEvent = EventType.valueOf(servicesCardSagaEventState.getSagaEventState());
      EventOutcome eventOutcome = EventOutcome.valueOf(servicesCardSagaEventState.getSagaEventOutcome());
      Event event = Event.builder()
              .eventOutcome(eventOutcome)
              .eventType(currentEvent)
              .eventPayload(servicesCardSagaEventState.getSagaEventResponse())
              .build();
      Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(currentEvent, eventOutcome);
      if (sagaEventState.isPresent()) {
        log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
        invokeNextEvent(event, servicesCardSaga, t, sagaEventState.get());
      }
    }
  }

  /**
   * This method will restart the saga process from the beginning. which could occur due to various reasons
   *
   * @param servicesCardSaga the model object.
   * @param t              the payload string as an object
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  private void replayFromBeginning(ServicesCardSaga servicesCardSaga, T t) throws InterruptedException, TimeoutException, IOException {
    Event event = Event.builder()
            .eventOutcome(INITIATE_SUCCESS)
            .eventType(INITIATED)
            .build();
    Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(INITIATED, INITIATE_SUCCESS);
    if (sagaEventState.isPresent()) {
      log.trace(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.get().getNextEventType(), event.toString());
      invokeNextEvent(event, servicesCardSaga, t, sagaEventState.get());
    }
  }

  /**
   * this method is called if there is a new message on this specific topic which this service is listening.
   *
   * @param event the event in the topic received as a json string and then converted to {@link Event}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */

  @Async
  @Transactional
  public void executeSagaEvent(@NotNull Event event) throws InterruptedException, IOException, TimeoutException {
    log.trace("executing saga event {}", event);
    Optional<ServicesCardSaga> sagaOptional = getSagaService().findSagaById(event.getSagaId());
    if (sagaOptional.isPresent() && !COMPLETED.toString().equalsIgnoreCase(sagaOptional.get().getStatus())) { //possible duplicate message.
      val saga = sagaOptional.get();
      Optional<SagaEventState<T>> sagaEventState = findNextSagaEventState(event.getEventType(), event.getEventOutcome());
      log.trace("found next event as {}", sagaEventState);
      if (sagaEventState.isPresent()) {
        process(event, saga, sagaEventState.get());
      } else {
        log.error("This should not have happened, please check that both the saga api and all the participating apis are in sync in terms of events and their outcomes. {}", event.toString()); // more explicit error message,
      }
    }
  }

  /**
   * this method will invoke the next event in the {@link BaseOrchestrator#nextStepsToExecute}
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaData       the payload string
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void invokeNextEvent(Event event, ServicesCardSaga saga, T sagaData, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    SagaStep<T> stepToExecute = sagaEventState.getStepToExecute();
    stepToExecute.apply(event, saga, sagaData);
  }


  /**
   * this method starts the process of saga event execution.
   *
   * @param event          the current event.
   * @param saga           the model object.
   * @param sagaEventState the next next event from {@link BaseOrchestrator#nextStepsToExecute}
   * @throws InterruptedException if thread is interrupted.
   * @throws IOException          if there is connectivity problem
   * @throws TimeoutException     if connection to messaging system times out.
   */
  protected void process(@NotNull Event event, ServicesCardSaga saga, SagaEventState<T> sagaEventState) throws InterruptedException, TimeoutException, IOException {
    T sagaData = JsonUtil.getJsonObjectFromString(clazz, saga.getPayload());
    if (!saga.getSagaState().equalsIgnoreCase(COMPLETED.toString())
            && isNotProcessedEvent(event.getEventType(), saga, this.nextStepsToExecute.keySet())) {
      log.info(SYSTEM_IS_GOING_TO_EXECUTE_NEXT_EVENT_FOR_CURRENT_EVENT, sagaEventState.getNextEventType(), event.toString());
      invokeNextEvent(event, saga, sagaData, sagaEventState);
    } else {
      log.info("ignoring this message as we have already processed it or it is completed. {}", event.toString()); // it is expected to receive duplicate message in saga pattern, system should be designed to handle duplicates.
    }
  }


  /**
   * this method finds the next event that needs to be executed.
   *
   * @param currentEvent current event
   * @param eventOutcome event outcome.
   * @return {@link Optional<SagaEventState>}
   */
  protected Optional<SagaEventState<T>> findNextSagaEventState(EventType currentEvent, EventOutcome eventOutcome) {
    val sagaEventStates = nextStepsToExecute.get(currentEvent);
    return sagaEventStates.stream().filter(el -> el.getCurrentEventOutcome() == eventOutcome).findFirst();
  }

  /**
   * abstract method for the child classes to implement, it needs to be populated which will help the saga to go through the steps.
   * it updates the contents of  {@link BaseOrchestrator#nextStepsToExecute}
   */
  protected abstract void populateStepsToExecuteMap();
}
