package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
class TaskManagementServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @Autowired
    private CamundaService camundaService;
    @MockBean
    private CamundaQueryBuilder camundaQueryBuilder;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    private TaskManagementService taskManagementService;
    private String taskId;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;

    private ServiceMocks mockServices;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        taskManagementService = new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService
        );

        mockServices.mockServiceAPIs();

        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            IDAM_USER_ID
            )
        ).thenReturn(true);

    }

    protected Map<String, CamundaVariable> createMockCamundaVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("SC", "String"));
        variables.put("title", new CamundaVariable("some_title", "String"));
        variables.put("executionType", new CamundaVariable("some_executionType", "String"));
        variables.put("taskSystem", new CamundaVariable("some_taskSystem", "String"));
        variables.put("jurisdiction", new CamundaVariable("some_jurisdiction", "String"));
        variables.put("region", new CamundaVariable("some_region", "String"));
        variables.put("appealType", new CamundaVariable("some_appealType", "String"));
        variables.put("autoAssigned", new CamundaVariable("false", "Boolean"));
        variables.put("assignee", new CamundaVariable("uid", "String"));
        return variables;
    }

    protected CamundaTask createMockedUnmappedTask() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            IDAM_USER_ID,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    void verifyTransactionWasRolledBack(String taskId) {
        transactionHelper.doInNewTransaction(() -> {
            //Find the task
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);

            assertNotNull(savedTaskResource);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(taskId, savedTaskResource.get().getTaskId());
            assertEquals("taskName", savedTaskResource.get().getTaskName());
            assertEquals("taskType", savedTaskResource.get().getTaskType());

            //Because transaction was rolled back we should have a state of unconfigured
            assertEquals(UNCONFIGURED, savedTaskResource.get().getState());
        });
    }

    private void createAndSaveTestTask(String taskId) {
        transactionHelper.doInNewTransaction(() -> {
            TaskResource taskResource = new TaskResource(
                taskId,
                "taskName",
                "taskType",
                UNCONFIGURED
            );
            taskResourceRepository.save(taskResource);
        });
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {
        @Test
        void cancelTask_should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(CANCEL)
            )).thenReturn(true);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            createAndSaveTestTask(taskId);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verifyTransactionWasRolledBack(taskId);

        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        @Test
        void completeTask_should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                IDAM_USER_ID,
                IDAM_USER_ID,
                mockedVariables,
                roleAssignment,
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            createAndSaveTestTask(taskId);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. Unable to update task state to completed.");

            verifyTransactionWasRolledBack(taskId);

        }

        @Test
        void completeTask_should_rollback_transaction_when_exception_occurs_calling_camunda_complete() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                IDAM_USER_ID,
                IDAM_USER_ID,
                mockedVariables,
                roleAssignment,
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            createAndSaveTestTask(taskId);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete partially succeeded. "
                            + "The Task state was updated to completed, but the Task could not be completed.");

            verifyTransactionWasRolledBack(taskId);

        }
    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class CompleteTaskWithPrivilegeAndCompletionOptions {

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(true);


                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

                createAndSaveTestTask(taskId);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true)
                    )))
                    .isInstanceOf(TaskAssignAndCompleteException.class)
                    .hasNoCause()
                    .hasMessage(
                        "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                        + "The Task was assigned to the user making the request but the Task could not be completed.");

                verifyTransactionWasRolledBack(taskId);

            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
                    IDAM_USER_ID,
                    IDAM_USER_ID,
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).completeTask(any(), any(), any());

                createAndSaveTestTask(taskId);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false)
                    )))
                    .isInstanceOf(TaskCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Complete Error: Task complete partially succeeded. "
                                + "The Task state was updated to completed, but the Task could not be completed.");

                verifyTransactionWasRolledBack(taskId);
            }
        }
    }

    @Nested
    @DisplayName("terminateTask()")
    class TerminateTask {

        @Nested
        @DisplayName("when terminate reason is completed")
        class Completed {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).searchHistory(any(), any());

                createAndSaveTestTask(taskId);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo(TerminateReason.COMPLETED)
                    )))
                    .isInstanceOf(ServerErrorException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage("There was a problem when deleting the historic cftTaskState");

                verifyTransactionWasRolledBack(taskId);

            }

        }

        @Nested
        @DisplayName("when terminate reason is cancelled")
        class Cancelled {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() throws Exception {

                createAndSaveTestTask(taskId);
                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).searchHistory(any(), any());

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () ->
                        taskManagementService.terminateTask(
                            taskId,
                            new TerminateInfo(TerminateReason.CANCELLED)
                        ))
                )
                    .isInstanceOf(ServerErrorException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage("There was a problem when deleting the historic cftTaskState");

                verifyTransactionWasRolledBack(taskId);
            }
        }
    }
}