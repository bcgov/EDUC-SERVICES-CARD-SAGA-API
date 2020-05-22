package ca.bc.gov.educ.api.saga.servicescard.repository;

import ca.bc.gov.educ.api.saga.servicescard.model.ServicesCardSaga;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServicesCardSagaRepository extends CrudRepository<ServicesCardSaga, UUID> {
  List<ServicesCardSaga> findAllByStatusIn(List<String> sagaState);

  List<ServicesCardSaga> findAll();
}
