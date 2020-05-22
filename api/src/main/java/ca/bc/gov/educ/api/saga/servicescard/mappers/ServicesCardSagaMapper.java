package ca.bc.gov.educ.api.saga.servicescard.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface ServicesCardSagaMapper {
  ServicesCardSagaMapper mapper = Mappers.getMapper(ServicesCardSagaMapper.class);
}
