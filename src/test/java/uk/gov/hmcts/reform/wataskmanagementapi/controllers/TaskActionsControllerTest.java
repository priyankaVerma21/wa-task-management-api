package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice.ErrorMessage;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTaskResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.NoRoleAssignmentsFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@ExtendWith(MockitoExtension.class)
class TaskActionsControllerTest {

    private static final String IDAM_AUTH_TOKEN = "IDAM_AUTH_TOKEN";
    @Mock
    private CamundaService camundaService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private Assignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private SystemDateProvider systemDateProvider;

    private TaskActionsController taskActionsController;

    @BeforeEach
    void setUp() {

        taskActionsController = new TaskActionsController(
            camundaService,
            accessControlService,
            systemDateProvider
        );

    }

    @Test
    void should_succeed_when_fetching_a_task_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();

        Task mockedTask = mock(Task.class);

        when(accessControlService.getRoles(IDAM_AUTH_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));

        when(camundaService.getTask(taskId, singletonList(mockedRoleAssignment), singletonList(PermissionTypes.READ)))
            .thenReturn(mockedTask);

        ResponseEntity<GetTaskResponse<Task>> response = taskActionsController.getTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), instanceOf(GetTaskResponse.class));
        assertNotNull(response.getBody());
        assertEquals(mockedTask, response.getBody().getTask());
    }

    @Test
    void should_succeed_when_claiming_a_task_and_return_a_204_no_content() {

        String taskId = UUID.randomUUID().toString();

        ResponseEntity<Void> response = taskActionsController.claimTask(IDAM_AUTH_TOKEN, taskId);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_unclaim_a_task_204_no_content() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";

        ResponseEntity<Void> response = taskActionsController.unclaimTask(authToken, taskId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_succeed_and_return_a_204_no_content_when_assigning_task() {

        String taskId = UUID.randomUUID().toString();
        String authToken = "someAuthToken";

        ResponseEntity<Void> response = taskActionsController.assignTask(
            authToken,
            taskId,
            new AssigneeRequest("userId")
        );

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_complete_a_task() {
        String taskId = UUID.randomUUID().toString();
        ResponseEntity response = taskActionsController.completeTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void should_cancel_a_task() {
        String taskId = UUID.randomUUID().toString();
        ResponseEntity response = taskActionsController.cancelTask(IDAM_AUTH_TOKEN, taskId);
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void exception_handler_should_return_403_for_no_role_assignments_found_exception() {

        final String exceptionMessage = "Some exception message";
        final NoRoleAssignmentsFoundException exception =
            new NoRoleAssignmentsFoundException(exceptionMessage);

        String mockedTimestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        when(systemDateProvider.nowWithTime()).thenReturn(mockedTimestamp);

        ResponseEntity<ErrorMessage> response = taskActionsController.handleNoRoleAssignmentsException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(mockedTimestamp, response.getBody().getTimestamp());
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), response.getBody().getError());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
        assertEquals(exceptionMessage, response.getBody().getMessage());

    }
}