package ca.bc.gov.educ.api.saga.servicescard.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
  private JsonUtil(){
  }
  public static String getJsonStringFromObject(Object payload) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsString(payload);
  }

  public static <T> T getJsonObjectFromString(Class<T> clazz,  String payload) throws JsonProcessingException {
    return new ObjectMapper().readValue(payload,clazz);
  }
}
