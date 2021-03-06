package uk.gov.hmcts.reform.managecase.client.prd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionalUser {

    private String userIdentifier;
    private String firstName;
    private String lastName;
    private String email;
    private String idamStatus;

}
