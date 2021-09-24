package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;

@ExtendWith(MockitoExtension.class)
class CancelTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    @Mock
    CamundaService camundaService;
    @Mock
    CamundaQueryBuilder camundaQueryBuilder;
    @Mock
    PermissionEvaluatorService permissionEvaluatorService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    TaskManagementService taskManagementService;
    String taskId;

    @Test
    void cancelTask_should_succeed_and_feature_flag_is_on() {

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

        TaskResource taskResource = spy(TaskResource.class);

        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            IDAM_USER_ID
            )
        ).thenReturn(true);

        taskManagementService.cancelTask(taskId, accessControlResponse);

        assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
        verify(camundaService, times(1)).cancelTask(taskId);
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
    }

    @Test
    void cancelTask_should_succeed_and_feature_flag_is_off() {

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

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            IDAM_USER_ID
            )
        ).thenReturn(false);

        taskManagementService.cancelTask(taskId, accessControlResponse);

        verify(camundaService, times(1)).cancelTask(taskId);
    }

    @Test
    void cancelTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

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
        )).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.cancelTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).cancelTask(any());
    }

    @Test
    void cancelTask_should_throw_exception_when_missing_required_arguments() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());
        assertThatThrownBy(() -> taskManagementService.cancelTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("UserId cannot be null");
    }

    @Test
    void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {

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

        TaskResource taskResource = spy(TaskResource.class);

        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.empty());

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            IDAM_USER_ID
            )
        ).thenReturn(true);

        assertThatThrownBy(() -> taskManagementService.cancelTask(taskId, accessControlResponse))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasNoCause()
            .hasMessage("Resource not found");
        verify(camundaService, times(0)).cancelTask(any());
        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
    }

    @BeforeEach
    public void setUp() {
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

        taskId = UUID.randomUUID().toString();
    }
}
