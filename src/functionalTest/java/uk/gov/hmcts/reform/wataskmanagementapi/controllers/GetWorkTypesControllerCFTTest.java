package uk.gov.hmcts.reform.wataskmanagementapi.controllers;


import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class GetWorkTypesControllerCFTTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "work-types";

    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider
            .getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
    }

    @Test
    public void should_return_work_types_when_user_has_work_types() {
        common.setupOrganisationalRoleAssignmentWithWorkTypes(authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "/?filter-by-user=true",
            authenticationHeaders
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        List<Map<String, String>> expectedWarnings = Lists.list(
            Map.of("id", "hearing_work", "label", "Hearing work"),
            Map.of("id", "upper_tribunal", "label", "Upper Tribunal"),
            Map.of("id", "routine_work", "label", "Routine work")
        );
        Assertions.assertEquals(expectedWarnings, workTypes);

    }

    @Test
    public void should_return_empty_work_types_when_user_has_no_work_types() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "/?filter-by-user=true",
            authenticationHeaders
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        Assertions.assertTrue(workTypes.isEmpty());

    }

    @Test
    public void should_return_all_work_types() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "/?filter-by-user=false",
            authenticationHeaders
        );
        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .and().contentType(MediaType.APPLICATION_JSON_VALUE);

        final List<Map<String, String>> workTypes = result.jsonPath().getList(
            "work_types");

        List<Map<String, String>> expectedWorkTypes = Lists.list(
            Map.of("id", "hearing_work", "label", "Hearing work"),
            Map.of("id", "upper_tribunal", "label", "Upper Tribunal"),
            Map.of("id", "routine_work", "label", "Routine work"),
            Map.of("id", "decision_making_work", "label","Decision-making work"),
            Map.of("id", "applications", "label","Applications"),
            Map.of("id", "priority", "label","Priority"),
            Map.of("id", "access_requests", "label","Access requests"),
            Map.of("id", "error_management", "label","Error management")
        );
        Assertions.assertEquals(expectedWorkTypes, workTypes);

    }

    @Test
    public void should_return_a_500_when_the_user_did_not_have_any_roles() {

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED + "/?filter-by-user=true",
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                                                     .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}