package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.DeleteTasksResponse;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.buildDeleteTasksResponse;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getEligibleForCancellationTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getTerminatedTaskIds;

@ExtendWith(MockitoExtension.class)
class DeleteTasksServiceHelperTest {

    private List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders;

    @BeforeEach
    void setUp() {
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);

        taskResourceCaseQueryBuilders = List.of(taskResourceCaseQueryBuilder1, taskResourceCaseQueryBuilder2);
    }

    @Test
    void shouldBuildDeleteTasksResponse() {

        final List<String> notTerminatedTasks = List.of("1");
        final List<String> terminatedTaskIds = List.of("2");
        final int failedToDeleteTasks = 1;

        final DeleteTasksResponse deleteTasksResponse =
                buildDeleteTasksResponse(taskResourceCaseQueryBuilders, notTerminatedTasks,
                        terminatedTaskIds, failedToDeleteTasks);

        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getCaseTasksFound()).isEqualTo(2);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getEligibleForCancellationTasks()).isEqualTo(1);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getDeletedCaseTasks()).isEqualTo(0);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getFailedCaseTasks()).isEqualTo(1);
    }

    @Test
    void shouldReturnTerminatedTaskIds() {

        when(taskResourceCaseQueryBuilders.get(0).getState()).thenReturn(CFTTaskState.TERMINATED);
        when(taskResourceCaseQueryBuilders.get(1).getState()).thenReturn(CFTTaskState.CANCELLED);
        when(taskResourceCaseQueryBuilders.get(0).getTaskId()).thenReturn("1");

        final List<String> terminatedTaskIds = getTerminatedTaskIds(taskResourceCaseQueryBuilders);

        assertThat(terminatedTaskIds.size()).isEqualTo(1);
        assertThat(terminatedTaskIds.get(0)).isEqualTo("1");
    }

    @Test
    void shouldReturnEligibleOfCancellationTaskIds() {

        when(taskResourceCaseQueryBuilders.get(0).getState()).thenReturn(CFTTaskState.UNASSIGNED);
        when(taskResourceCaseQueryBuilders.get(1).getState()).thenReturn(CFTTaskState.TERMINATED);
        when(taskResourceCaseQueryBuilders.get(0).getTaskId()).thenReturn("1");

        final List<String> terminatedTaskIds = getEligibleForCancellationTaskIds(taskResourceCaseQueryBuilders);

        assertThat(terminatedTaskIds.size()).isEqualTo(1);
        assertThat(terminatedTaskIds.get(0)).isEqualTo("1");
    }
}