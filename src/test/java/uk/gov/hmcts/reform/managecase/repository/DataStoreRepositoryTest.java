package uk.gov.hmcts.reform.managecase.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ES_QUERY;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.NOC_REQUEST_DESCRIPTION;

class DataStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ROLE = "caseworker-probate";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String ORG_ID = "organisation1";
    private static final String EVENT_ID = "NoCRequest";
    private static final String EXPECTED_NOC_REQUEST_DATA =
        "{"
            + "\"DummyDataKey\": \"Dummy Data Value\""
            + "}";

    @Mock
    private DataStoreApiClient dataStoreApi;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DefaultDataStoreRepository repository;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    @DisplayName("Find case by caseTypeId and caseId")
    void shouldFindCaseBy() {
        // ARRANGE
        CaseDetails caseDetails = CaseDetails.builder()
                .caseTypeId(CASE_TYPE_ID)
                .reference(CASE_ID)
                .build();
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList(caseDetails));
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        // ACT
        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).get().isEqualTo(caseDetails);

        verify(dataStoreApi).searchCases(eq(CASE_TYPE_ID), eq(String.format(ES_QUERY, CASE_ID)));
    }

    @Test
    @DisplayName("Find case return no cases")
    void shouldReturnNoCaseForSearch() {
        // ARRANGE
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList());
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        // ACT
        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).isNotPresent();
    }

    @Test
    @DisplayName("Assign case access")
    void shouldAssignCase() {
        // ARRANGE
        doNothing().when(dataStoreApi).assignCase(any(CaseUserRolesRequest.class));

        // ACT
        repository.assignCase(List.of(ROLE), CASE_ID, ASSIGNEE_ID, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).assignCase(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRoles = captor.getValue().getCaseUsers();

        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRoleWithOrganisation caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(caseUserRole.getOrganisationId()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("Get case assignments")
    void shouldGetCaseAssignments() {
        // ARRANGE
        List<String> caseIds = List.of(CASE_ID);
        List<String> userIds = List.of(ASSIGNEE_ID);

        CaseUserRole inputRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(ASSIGNEE_ID)
                .caseRole(ROLE)
                .build();

        given(dataStoreApi.getCaseAssignments(caseIds, userIds))
                .willReturn(new CaseUserRoleResource(List.of(inputRole)));

        // ACT
        List<CaseUserRole> caseUserRoles = repository.getCaseAssignments(caseIds, userIds);

        // ASSERT
        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRole caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
    }

    @Test
    @DisplayName("Remove case user roles")
    void shouldRemoveCaseUserRoles() {
        // ARRANGE
        doNothing().when(dataStoreApi).removeCaseUserRoles(any(CaseUserRolesRequest.class));

        List<CaseUserRole> caseUserRoles = List.of(
            new CaseUserRole(CASE_ID, ASSIGNEE_ID, ROLE),
            new CaseUserRole(CASE_ID2, ASSIGNEE_ID, ROLE)
        );

        // ACT
        repository.removeCaseUserRoles(caseUserRoles, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).removeCaseUserRoles(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRolesWithOrganisation = captor.getValue().getCaseUsers();

        assertThat(caseUserRolesWithOrganisation)
            .hasSameSizeAs(caseUserRoles)
            .containsAll(List.of(
                new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, ROLE, ORG_ID),
                new CaseUserRoleWithOrganisation(CASE_ID2, ASSIGNEE_ID, ROLE, ORG_ID)
            ));
    }

    @Test
    @DisplayName("Call ccd-datastore where submitting an event for a case fails")
    void shouldReturnNullCaseResourceWhenSubmittingEventFails() {
        // ARRANGE
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder().build();

        given(dataStoreApi.getStartEventTrigger(CASE_ID, EVENT_ID))
            .willReturn(null);

        // ACT
        CaseResource returnedStartEventResource
            = repository.submitEventForCase(CASE_ID, EVENT_ID, changeOrganisationRequest);

        // ASSERT
        verify(dataStoreApi).getStartEventTrigger(CASE_ID, EVENT_ID);

        assertThat(returnedStartEventResource).isNull();
    }

    @Test
    @DisplayName("Call ccd-datastore to submit an event for a case")
    void shouldCaseResourceWhenSubmittingEventSucceeds() throws JsonProcessingException {
        // ARRANGE
        StartEventResource startEventResource = new StartEventResource();
        startEventResource.setToken("eventToken");

        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .organisationToAdd(new Organisation("1", "orgNameToAdd"))
            .organisationToRemove(new Organisation("2", "orgNameToRemove"))
            .requestTimestamp(LocalDateTime.now())
            .build();

        given(dataStoreApi.getStartEventTrigger(CASE_ID, EVENT_ID))
            .willReturn(startEventResource);
        given(dataStoreApi.submitEventForCase(any(String.class), any(CaseDataContent.class)))
            .willReturn(CaseResource.builder().build());
        given(objectMapper.writeValueAsString(changeOrganisationRequest))
            .willReturn(EXPECTED_NOC_REQUEST_DATA);

        // ACT
        repository.submitEventForCase(CASE_ID, EVENT_ID, changeOrganisationRequest);

        // ASSERT
        verify(dataStoreApi).getStartEventTrigger(CASE_ID, EVENT_ID);
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        verify(dataStoreApi).submitEventForCase(any(String.class), captor.capture());

        CaseDataContent caseDataContentCaptorValue = captor.getValue();
        assertThat(caseDataContentCaptorValue.getToken()).isEqualTo("eventToken");

        assertThat(caseDataContentCaptorValue.getEvent().getDescription())
            .isEqualTo(NOC_REQUEST_DESCRIPTION);
        assertThat(caseDataContentCaptorValue.getEvent().getEventId())
            .isEqualTo(EVENT_ID);

        assertThat(caseDataContentCaptorValue.getData().get("ChangeOrganisationRequest").asText())
            .isEqualTo(EXPECTED_NOC_REQUEST_DATA);
    }

    @Test
    @DisplayName("find case by id using external facing API")
    void shouldFindCaseByIdUsingExternalApi() {
        // ARRANGE
        CaseResource caseResource = CaseResource.builder().build();

        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID)).willReturn(caseResource);

        // ACT
        CaseResource result = repository.findCaseByCaseIdExternalApi(CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseResource);

        verify(dataStoreApi).getCaseDetailsByCaseIdViaExternalApi(eq(CASE_ID));

    }
}
