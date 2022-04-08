package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.data.RoleAssignmentCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskPermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@ExtendWith(MockitoExtension.class)
public class CftQueryServiceTest extends CamundaHelpers {

    @Mock
    private TaskResourceRepository taskResourceRepository;
    @Mock
    private CFTTaskMapper cftTaskMapper;
    @Mock
    private CamundaService camundaService;

    @InjectMocks
    private CftQueryService cftQueryService;

    private TaskResource createTaskResource() {
        return new TaskResource(
            "taskId",
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.COMPLETED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            "1623278362430412",
            "Asylum",
            "TestCase",
            "IA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            singleton(new TaskRoleResource(
                "tribunal-caseworker",
                true,
                false,
                false,
                false,
                false,
                false,
                new String[]{"SPECIFIC", "BASIC"},
                0,
                false,
                "JUDICIAL",
                "taskId",
                OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
            )),
            "caseCategory"
        );
    }

    private Task getTask() {
        return new Task(
            "4d4b6fgh-c91f-433f-92ac-e456ae34f72a",
            "Review the appeal",
            "reviewTheAppeal",
            "assigned",
            "SELF",
            "PUBLIC",
            "Review the appeal",
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            "10bac6bf-80a7-4c81-b2db-516aba826be6",
            true,
            "Case Management Task",
            "IA",
            "1",
            "765324",
            "Taylor House",
            "Asylum",
            "1617708245335311",
            "refusalOfHumanRights",
            "Bob Smith",
            true,
            null,
            "Some Case Management Category",
            "hearing_work",
            new TaskPermissions(new HashSet<>(singleton(PermissionTypes.READ))),
            RoleCategory.LEGAL_OPERATIONS.name(),
            "Description"
        );
    }

    private static List<RoleAssignment> roleAssignmentWithAllGrantTypes(Classification classification) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignment roleAssignment = RoleAssignment.builder().roleName("hmcts-judiciary")
            .classification(classification)
            .grantType(GrantType.BASIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> specificAttributes = Map.of(
            RoleAttributeDefinition.CASE_TYPE.value(), "Asylum",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(specificAttributes)
            .grantType(GrantType.SPECIFIC)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> stdAttributes = Map.of(
            RoleAttributeDefinition.REGION.value(), "1",
            RoleAttributeDefinition.JURISDICTION.value(), "IA",
            RoleAttributeDefinition.BASE_LOCATION.value(), "765324"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(stdAttributes)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> challengedAttributes = Map.of(
            RoleAttributeDefinition.JURISDICTION.value(), "IA"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(challengedAttributes)
            .authorisations(List.of("DIVORCE", "PROBATE"))
            .grantType(GrantType.CHALLENGED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        final Map<String, String> excludeddAttributes = Map.of(
            RoleAttributeDefinition.CASE_ID.value(), "1623278362431003"
        );
        roleAssignment = RoleAssignment.builder().roleName("senior-tribunal-caseworker")
            .classification(classification)
            .attributes(excludeddAttributes)
            .grantType(GrantType.EXCLUDED)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
        roleAssignments.add(roleAssignment);

        return roleAssignments;
    }

    @Nested
    @DisplayName("searchForTasks()")
    class SearchForTasks {
        @Test
        void shouldReturnAllTasks() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, asList("hearing_work"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            Page<TaskResource> taskResources = new PageImpl<>(List.of(createTaskResource()));
            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());
            when(taskResourceRepository.findAll(any(), any(Pageable.class))).thenReturn(taskResources);

            GetTasksResponse<Task> taskResourceList
                = cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired);

            assertNotNull(taskResourceList);
            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", taskResourceList.getTasks().get(0).getId());
            assertEquals("hearing_work", taskResourceList.getTasks().get(0).getWorkTypeId());

            verify(taskResourceRepository, times(1)).findAll(any(), any(Pageable.class));
        }

        @Test
        void shouldReturnAllTasksWhenWorkTypeIsNotSent() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            Page<TaskResource> taskResources = new PageImpl<>(List.of(createTaskResource()));
            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());
            when(taskResourceRepository.findAll(any(), any(Pageable.class))).thenReturn(taskResources);

            GetTasksResponse<Task> taskResourceList
                = cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired);

            assertNotNull(taskResourceList);
            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", taskResourceList.getTasks().get(0).getId());

            verify(taskResourceRepository, times(1)).findAll(any(), any(Pageable.class));
        }

        @Test
        void shouldReturnAvailableTasksOnly() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003")),
                new SearchParameterList(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, asList("true"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            Page<TaskResource> taskResources = new PageImpl<>(List.of(createTaskResource()));
            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());
            when(taskResourceRepository.findAll(any(), any(Pageable.class))).thenReturn(taskResources);

            GetTasksResponse<Task> taskResourceList
                = cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired);

            assertNotNull(taskResourceList);
            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", taskResourceList.getTasks().get(0).getId());
            assertEquals("hearing_work", taskResourceList.getTasks().get(0).getWorkTypeId());

            verify(taskResourceRepository, times(1)).findAll(any(), any(Pageable.class));
        }

        @Test
        void shouldThrowExceptionWhenInvalidWorkTypeIsSent() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003")),
                new SearchParameterList(WORK_TYPE, SearchOperator.IN, asList("unknown"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            assertThrows(
                CustomConstraintViolationException.class, () ->
                    cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired)
            );

            verify(cftTaskMapper, Mockito.never()).mapToTaskWithPermissions(any(), any());
            verify(taskResourceRepository, Mockito.never()).findAll(any(), any(Pageable.class));
        }

        @Test
        void shouldReturnAllTasksWithNullValues() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            final TaskResource taskResource = createTaskResource();

            Page<TaskResource> taskResources = new PageImpl<>(List.of(taskResource));
            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());
            when(taskResourceRepository.findAll(any(), any(Pageable.class))).thenReturn(taskResources);

            GetTasksResponse<Task> taskResourceList
                = cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired);

            assertNotNull(taskResourceList);
            final Task task = taskResourceList.getTasks().get(0);

            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", task.getId());
            assertNotNull(task.getCreatedDate());
            assertNotNull(task.getDueDate());

            verify(taskResourceRepository, times(1)).findAll(any(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyListWhenNoTasksInDatabase() {
            final SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            Page<TaskResource> taskResources = Page.empty();
            when(taskResourceRepository.findAll(any(), any(Pageable.class))).thenReturn(taskResources);
            //when(cftTaskMapper.mapToTask(any())).thenReturn(getTask());

            GetTasksResponse<Task> taskResourceList
                = cftQueryService.searchForTasks(1, 10, searchTaskRequest, accessControlResponse, permissionsRequired);

            assertNotNull(taskResourceList);
            assertTrue(taskResourceList.getTasks().isEmpty());

        }

        @Test
        void should_raise_exception_when_invalid_offset() {
            SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            assertThatThrownBy(() -> cftQueryService.searchForTasks(
                -1,
                25,
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Offset index must not be less than zero");
        }

        @Test
        void should_raise_exception_when_invalid_limit() {
            SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
                new SearchParameterList(JURISDICTION, SearchOperator.IN, asList("IA")),
                new SearchParameterList(LOCATION, SearchOperator.IN, asList("765324")),
                new SearchParameterList(STATE, SearchOperator.IN, asList("ASSIGNED")),
                new SearchParameterList(USER, SearchOperator.IN, asList("TEST")),
                new SearchParameterList(CASE_ID, SearchOperator.IN, asList("1623278362431003"))
            ),
                List.of(new SortingParameter(SortField.CASE_ID_SNAKE_CASE, SortOrder.ASCENDANT)));

            AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            assertThatThrownBy(() -> cftQueryService.searchForTasks(
                0,
                0,
                searchTaskRequest,
                accessControlResponse,
                permissionsRequired
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Limit must not be less than one");
        }

    }

    @Nested
    @DisplayName("getTask()")
    class GetTask {

        @Test
        void shouldGetTask() {
            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            String taskId = "taskId";
            TaskResource expectedTask = new TaskResource(
                taskId,
                "takeName",
                "taskType",
                UNCONFIGURED,
                "caseId"
            );

            when(taskResourceRepository.findOne(any())).thenReturn(Optional.of(expectedTask));
            Optional<TaskResource> returnedTask =
                cftQueryService.getTask(taskId, accessControlResponse, permissionsRequired);

            assertNotNull(returnedTask);
            assertEquals(expectedTask, returnedTask.get());

            verify(taskResourceRepository, times(1)).findOne(any());
        }

        @Test
        void shouldReturnEmptyTaskResourceWhenTaskIdIsEmpty() {
            final AccessControlResponse accessControlResponse = new AccessControlResponse(
                null,
                roleAssignmentWithAllGrantTypes(Classification.PUBLIC)
            );
            List<PermissionTypes> permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);

            Optional<TaskResource> returnedTask =
                cftQueryService.getTask("", accessControlResponse, permissionsRequired);

            assertTrue(returnedTask.isEmpty());
        }

    }

    @Nested
    @DisplayName("searchForCompletableTasks()")
    class SearchForCompletableTasks {

        private List<PermissionTypes> permissionsRequired;

        @BeforeEach
        public void setup() {
            permissionsRequired = new ArrayList<>();
            permissionsRequired.add(PermissionTypes.READ);
        }

        @Test
        void should_succeed_and_return_emptyList_when_jurisdiction_is_not_IA() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "invalidJurisdiction",
                "Asylum"
            );

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_caseType_is_not_Asylum() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "someInvalidCaseType"
            );

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_no_task_types_returned() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase)).thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_search_results() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );
            when(accessControlResponse.getRoleAssignments())
                .thenReturn(singletonList(RoleAssignmentCreator.aRoleAssignment().build()));
            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

            when(taskResourceRepository.findAll(any()))
                .thenReturn(List.of(createTaskResource()));

            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", response.getTasks().get(0).getId());
            assertTrue(response.isTaskRequiredForEvent());

            verify(taskResourceRepository, times(1)).findAll(any());
        }

        @Test
        void should_succeed_and_return_search_results_with_task_required_as_false() {
            AccessControlResponse accessControlResponse =
                new AccessControlResponse(null, singletonList(RoleAssignmentCreator.aRoleAssignment().build()));
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponses());

            when(camundaService.getVariableValue(any(), any()))
                .thenReturn("reviewTheAppeal");

            when(taskResourceRepository.findAll(any()))
                .thenReturn(List.of(createTaskResource()));

            when(cftTaskMapper.mapToTaskAndExtractPermissionsUnion(any(), any())).thenReturn(getTask());

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertFalse(response.getTasks().isEmpty());
            assertEquals("4d4b6fgh-c91f-433f-92ac-e456ae34f72a", response.getTasks().get(0).getId());
            assertFalse(response.isTaskRequiredForEvent());
        }

        @Test
        void should_succeed_and_return_emptyList_when_cft_db_returns_no_record() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

            when(taskResourceRepository.findAll(any()))
                .thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertTrue(response.getTasks().isEmpty());
            assertTrue(response.isTaskRequiredForEvent());

            verify(cftTaskMapper, times(0)).mapToTaskWithPermissions(any(), any());
        }

        @Test
        void should_succeed_and_returns_empty_results_when_dmn_returns_empty_variables() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertTrue(response.getTasks().isEmpty());
            assertFalse(response.isTaskRequiredForEvent());

            verify(camundaService, times(0)).getVariableValue(any(), any());
            verify(cftTaskMapper, times(0)).mapToTaskWithPermissions(any(), any());
            verify(taskResourceRepository, times(0)).findAll(any());
        }

        @Test
        void should_succeed_and_return_search_results1() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(camundaService.getVariableValue(any(), any())).thenReturn("");

            GetTasksCompletableResponse<Task> response = cftQueryService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse,
                permissionsRequired
            );

            assertNotNull(response);
            assertTrue(response.getTasks().isEmpty());
            assertFalse(response.isTaskRequiredForEvent());

            verify(cftTaskMapper, times(0)).mapToTaskWithPermissions(any(), any());
            verify(taskResourceRepository, times(0)).findAll(any());
        }

    }
}
