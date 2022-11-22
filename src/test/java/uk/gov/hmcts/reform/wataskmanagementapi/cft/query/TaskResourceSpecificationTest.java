package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import lombok.Builder;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.predicate.BooleanAssertionPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification.PUBLIC;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantType;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.query.RoleAssignmentTestUtils.roleAssignmentWithSpecificGrantTypeOnly;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.CASE_ID_CAMEL_CASE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.ROLE_CATEGORY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.USER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;

@ExtendWith(MockitoExtension.class)
public class TaskResourceSpecificationTest {

    @Mock(extraInterfaces = Serializable.class)
    Root<TaskResource> root;
    @Mock(extraInterfaces = Serializable.class)
    CriteriaQuery<?> query;
    @Mock(extraInterfaces = Serializable.class)
    CriteriaBuilderImpl criteriaBuilder;
    @Mock
    Join<Object, Object> taskRoleResources;
    @Mock
    CriteriaBuilder.In<Object> inObject;
    @Mock
    CriteriaBuilder.In<Object> values;
    @Mock
    Path<Object> authorizations;
    @Mock
    Path<Object> path;
    @Mock
    Predicate mockPredicate;

    private static final PermissionRequirements readPermissionsRequired = PermissionRequirementBuilder.builder()
        .buildSingleType(READ);
    private static final PermissionRequirements readOwnPermissionsRequired = PermissionRequirementBuilder.builder()
        .buildSingleRequirementWithAnd(READ, OWN);

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.or(any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any())).thenReturn(inObject);
        lenient().when(criteriaBuilder.and(any(), any(), any(), any())).thenReturn(inObject);
        BooleanAssertionPredicate booleanAssertionPredicate = new BooleanAssertionPredicate(
            criteriaBuilder,
            null,
            Boolean.TRUE
        );
        lenient().when(criteriaBuilder.conjunction()).thenReturn(booleanAssertionPredicate);
        lenient().when(criteriaBuilder.equal(any(), any())).thenReturn(mockPredicate);
        lenient().when(criteriaBuilder.equal(any(), anyString())).thenReturn(mockPredicate);
        lenient().when(criteriaBuilder.in(any())).thenReturn(inObject);
        lenient().when(inObject.value(any())).thenReturn(values);

        lenient().when(taskRoleResources.get(anyString())).thenReturn(authorizations);

        lenient().when(authorizations.isNull()).thenReturn(mockPredicate);
        lenient().when(root.join(anyString())).thenReturn(taskRoleResources);
        lenient().when(root.get(anyString())).thenReturn(path);
        lenient().when(root.get(anyString()).get(anyString())).thenReturn(path);
    }

    @ParameterizedTest
    @MethodSource({
        "searchParameterForTaskQuery"
    })
    void should_build_task_query_with_single_parameter(SearchTaskRequestScenario scenario) {
        AccessControlResponse accessControlResponse = new AccessControlResponse(
            null,
            roleAssignmentWithSpecificGrantTypeOnly(PUBLIC)
        );

        final Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            scenario.searchTaskRequest, accessControlResponse.getRoleAssignments(),
            scenario.permissionsRequired,
            scenario.availableTaskOnly,
            criteriaBuilder, root
        );
        assertNotNull(predicate);

        verify(criteriaBuilder, times(scenario.expectedEqualPredicate)).equal(any(), anyString());
        verify(criteriaBuilder, times(scenario.expectedConjunctions)).conjunction();
    }

    @Test
    void should_build_task_query_with_empty_search_parameters() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(emptyList());

        lenient().when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);

        Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readPermissionsRequired, false,
            criteriaBuilder, root
        );

        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).equal(any(), anyString());
        verify(criteriaBuilder, times(12)).conjunction();
    }

    @Test
    void should_build_task_query_with_search_parameters_as_null() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(null);
        lenient().when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);

        Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readPermissionsRequired, false,
            criteriaBuilder, root
        );

        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).equal(any(), anyString());
        verify(criteriaBuilder, times(12)).conjunction();
    }

    @Test
    void should_build_task_query_with_null_conditions() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList(null)),
            new SearchParameterList(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, singletonList(null))
        ));

        lenient().when(criteriaBuilder.conjunction()).thenReturn(mockPredicate);

        List<RoleAssignment> roleAssignments = roleAssignmentWithSpecificGrantTypeOnly(PUBLIC);
        Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignments, readPermissionsRequired, false,
            criteriaBuilder, root
        );

        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).equal(any(), anyString());
        verify(criteriaBuilder, times(12)).conjunction();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, emptyList())
        ));

        predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignments, readPermissionsRequired, false,
            criteriaBuilder, root
        );

        assertNotNull(predicate);

        predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            null, roleAssignments, readPermissionsRequired, false,
            criteriaBuilder, root
        );
        assertNotNull(predicate);
    }

    @Test
    void should_build_task_query_with_out_search_parameter_and_return_conjunction_as_null() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(emptyList());
        lenient().when(criteriaBuilder.conjunction()).thenReturn(null);

        Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readPermissionsRequired, false,
            criteriaBuilder, root
        );
        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).equal(any(), anyString());
        verify(criteriaBuilder, times(12)).conjunction();
    }

    @Test
    void should_build_task_query_with_all_parameters() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("case_id")),
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser")),
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("LEGAL_OPERATIONS")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, false)
        ));


        final Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readPermissionsRequired, false,
            criteriaBuilder, root
        );

        verify(criteriaBuilder, times(7)).equal(any(), anyString());
    }

    @Test
    void should_build_task_query_with_all_parameters_with_camel_case() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location")),
            new SearchParameterList(CASE_ID_CAMEL_CASE, SearchOperator.IN, singletonList("caseId")),
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser")),
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("LEGAL_OPERATIONS")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, false)
        ));


        final Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readPermissionsRequired, false,
            criteriaBuilder, root
        );

        verify(criteriaBuilder, times(7)).equal(any(), anyString());
    }

    @Test
    void should_build_task_query_with_available_task_only() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED")),
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location")),
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("case_id")),
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser")),
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work")),
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("LEGAL_OPERATIONS")),
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));

        List<PermissionTypes> permissionsRequired = new ArrayList<>();
        permissionsRequired.add(READ);

        final Predicate predicate = TaskSearchQueryBuilder.buildTaskSummaryQuery(
            searchTaskRequest, roleAssignmentWithSpecificGrantTypeOnly(PUBLIC), readOwnPermissionsRequired, true,
            criteriaBuilder, root
        );

        verify(criteriaBuilder, times(7)).equal(any(), anyString());

    }

    @Test
    void should_build_single_task_query() {
        final Predicate predicate = TaskSearchQueryBuilder.buildSingleTaskQuery(
            "someTaskId", roleAssignmentWithSpecificGrantType(PUBLIC),
            PermissionRequirementBuilder.builder().buildSingleType(READ),
            criteriaBuilder, root
        );

        assertNotNull(predicate);
    }

    @Test
    void should_build_task_role_permissions_query() {

        final Predicate predicate = TaskSearchQueryBuilder.buildTaskRolePermissionsQuery(
            "someTaskId", roleAssignmentWithSpecificGrantType(PUBLIC), criteriaBuilder, root);

        assertNotNull(predicate);
    }

    @ParameterizedTest
    @MethodSource({
        "searchParameterForCompletable"
    })
    void should_build_task_query_for_search_for_completable(SearchTaskRequestScenario scenario) {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);

        Predicate predicate = TaskSearchQueryBuilder.buildQueryForCompletable(
            scenario.searchEventAndCase,
            roleAssignmentWithSpecificGrantTypeOnly(PUBLIC),
            permissionsRequired,
            List.of("taskType"),
            criteriaBuilder,
            root
        );

        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).in(any());
        verify(criteriaBuilder, times(3)).equal(any(), anyString());

    }

    @Test
    void should_build_task_query_for_search_for_completable_negated() {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "IA", "caseType");

        Predicate predicate = TaskSearchQueryBuilder.buildQueryForCompletable(
            searchEventAndCase,
            roleAssignmentWithSpecificGrantTypeOnly(PUBLIC),
            permissionsRequired,
            emptyList(),
            criteriaBuilder,
            root
        );

        assertNotNull(predicate);

        verify(criteriaBuilder, times(1)).in(any());
        verify(criteriaBuilder, times(2)).equal(any(), anyString());
    }

    private static Stream<SearchTaskRequestScenario> searchParameterForTaskQuery() {
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(JURISDICTION, SearchOperator.IN, singletonList("IA"))
        ));
        final SearchTaskRequestScenario jurisdiction =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(STATE, SearchOperator.IN, singletonList("ASSIGNED"))
        ));
        final SearchTaskRequestScenario state =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(1).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, true)
        ));
        final SearchTaskRequestScenario availableTaskOnly =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readOwnPermissionsRequired).availableTaskOnly(true)
                .expectedEqualPredicate(1).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, SearchOperator.BOOLEAN, false)
        ));
        final SearchTaskRequestScenario availableTaskOnlyAsFalse =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(1).expectedConjunctions(12).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(LOCATION, SearchOperator.IN, singletonList("location"))
        ));
        final SearchTaskRequestScenario location =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(CASE_ID, SearchOperator.IN, singletonList("case_id"))
        ));
        final SearchTaskRequestScenario caseId =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(USER, SearchOperator.IN, singletonList("testUser"))
        ));
        final SearchTaskRequestScenario user =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(WORK_TYPE, SearchOperator.IN, singletonList("routine_work"))
        ));
        final SearchTaskRequestScenario workType =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        searchTaskRequest = new SearchTaskRequest(List.of(
            new SearchParameterList(ROLE_CATEGORY, SearchOperator.IN, singletonList("LEGAL_OPERATIONS"))
        ));
        final SearchTaskRequestScenario roleCtg =
            SearchTaskRequestScenario.builder().searchTaskRequest(searchTaskRequest)
                .permissionsRequired(readPermissionsRequired).availableTaskOnly(false)
                .expectedEqualPredicate(2).expectedConjunctions(11).build();

        return Stream.of(jurisdiction, state, location, caseId, user, workType, roleCtg,
            availableTaskOnly, availableTaskOnlyAsFalse
        );

    }

    private static Stream<SearchTaskRequestScenario> searchParameterForCompletable() {
        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "caseId", "eventId", "IA", "caseType");
        SearchTaskRequestScenario withCaseId =
            SearchTaskRequestScenario.builder().searchEventAndCase(searchEventAndCase).build();

        return Stream.of(withCaseId);
    }

    @Builder
    private static class SearchTaskRequestScenario {
        SearchTaskRequest searchTaskRequest;
        SearchEventAndCase searchEventAndCase;
        PermissionRequirements permissionsRequired;
        List<String> taskTypes;
        int expectedInPredicate;
        int expectedEqualPredicate;
        int expectedConjunctions;
        public Boolean availableTaskOnly;
    }
}
