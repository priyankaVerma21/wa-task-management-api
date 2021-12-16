package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestVariables;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class GetTaskByIdRolePermissionsTest extends SpringBootFunctionalBaseTest {

    private static final String TASK_INITIATION_ENDPOINT_BEING_TESTED = "task/{task-id}";
    private static final String ENDPOINT_BEING_TESTED = "task/{task-id}/roles";
    private Headers authenticationHeaders;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getTribunalCaseworkerAAuthorization("wa-ft-test-r2-");
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("timestamp", lessThanOrEqualTo(ZonedDateTime.now().plusSeconds(60)
                .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))))
            .body("error", equalTo(HttpStatus.UNAUTHORIZED.getReasonPhrase()))
            .body("status", equalTo(HttpStatus.UNAUTHORIZED.value()))
            .body("message", equalTo("User did not have sufficient permissions to perform this action"));

        common.cleanUpTask(taskId);

    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() {
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            nonExistentTaskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .and()
            .contentType(APPLICATION_PROBLEM_JSON_VALUE)
            .body("type", equalTo("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"))
            .body("title", equalTo("Task Not Found Error"))
            .body("status", equalTo(HttpStatus.NOT_FOUND.value()))
            .body("detail", equalTo("Task Not Found Error: The task could not be found."));
    }

    @Test
    public void should_return_200_and_retrieve_role_permission_information_for_given_task_id() {
        TestVariables taskVariables = common.setupTaskAndRetrieveIds();
        String taskId = taskVariables.getTaskId();
        common.setupOrganisationalRoleAssignment(authenticationHeaders);

        common.insertTaskInCftTaskDb(taskVariables, "followUpOverdueReasonsForAppeal", authenticationHeaders);

        Response result = restApiActions.get(
            ENDPOINT_BEING_TESTED,
            taskId,
            authenticationHeaders
        );

        result.then().assertThat()
            .statusCode(HttpStatus.OK.value())
            .contentType(APPLICATION_JSON_VALUE)
            .body("roles.size()", equalTo(5))
            .body("roles[0].role_category", is("LEGAL_OPERATIONS"))
            .body("roles[0].role_name", is("case-manager"))
            .body("roles[0].permissions", hasItems("Read", "Own", "Refer"))
            .body("roles[0].authorisations", empty())
            .body("roles[1].role_category", is("JUDICIAL"))
            .body("roles[1].role_name", is("judge"))
            .body("roles[1].permissions", hasItems("Read", "Refer", "Execute"))
            .body("roles[1].authorisations", hasItems("IA"))
            .body("roles[2].role_category", equalTo("LEGAL_OPERATIONS"))
            .body("roles[2].role_name", is("senior-tribunal-caseworker"))
            .body("roles[2].permissions", hasItems("Read", "Own", "Refer"))
            .body("roles[2].authorisations", empty())
            .body("roles[3].role_category", nullValue())
            .body("roles[3].role_name", is("task-supervisor"))
            .body("roles[3].permissions", hasItems("Read", "Cancel", "Manage", "Refer"))
            .body("roles[3].authorisations", empty())
            .body("roles[4].role_category", is("LEGAL_OPERATIONS"))
            .body("roles[4].role_name", is("tribunal-caseworker"))
            .body("roles[4].permissions", hasItems("Read", "Own", "Refer"))
            .body("roles[4].authorisations", empty());

        common.cleanUpTask(taskId);
    }

}