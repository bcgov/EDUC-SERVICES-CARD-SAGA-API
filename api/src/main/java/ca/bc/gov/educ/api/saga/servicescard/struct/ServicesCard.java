package ca.bc.gov.educ.api.saga.servicescard.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServicesCard implements Serializable {
  private static final long serialVersionUID = 1L;

  String servicesCardInfoID;
  String digitalIdentityID;
  String did;
  String userDisplayName;
  String givenName;
  String givenNames;
  String surname;
  String birthDate;
  String gender;
  String identityAssuranceLevel;
  String email;
  String streetAddress;
  String city;
  String province;
  String country;
  String postalCode;
  String updateDate;
  String updateUser;
  String subscriptionId;
}
