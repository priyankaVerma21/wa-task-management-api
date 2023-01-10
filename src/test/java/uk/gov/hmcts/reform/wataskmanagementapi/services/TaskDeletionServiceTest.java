package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.DeleteTasksResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@ExtendWith(MockitoExtension.class)
class TaskDeletionServiceTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private DeletedTasksDatabaseService deletedTasksDatabaseService;

    @InjectMocks
    private TaskDeletionService taskDeletionService;

    @Test
    void shouldDeletedTasksResponse() {
        final String caseId = "123";
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);

        when(cftTaskDatabaseService.findByTaskIdsByCaseId(caseId)).thenReturn(List.of(taskResourceCaseQueryBuilder1,
                taskResourceCaseQueryBuilder2));
        when(taskResourceCaseQueryBuilder1.getState()).thenReturn(TERMINATED);
        when(taskResourceCaseQueryBuilder2.getState()).thenReturn(TERMINATED);

        when(taskResourceCaseQueryBuilder1.getTaskId()).thenReturn("234");
        when(taskResourceCaseQueryBuilder2.getTaskId()).thenReturn("567");

        doNothing().when(cftTaskDatabaseService).deleteTask("234");
        doNothing().when(cftTaskDatabaseService).deleteTask("567");
        doNothing().when(deletedTasksDatabaseService).addProcessedTasks("123");

        final DeleteTasksResponse deleteTasksResponse = taskDeletionService.deleteTasksByCaseId(caseId);

        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getCaseTasksFound()).isEqualTo(2);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getDeletedCaseTasks()).isEqualTo(2);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getFailedCaseTasks()).isEqualTo(0);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getEligibleForCancellationTasks()).isEqualTo(0);
    }

    @Test
    void shouldReturnCanceledResponse() {
        final String caseId = "123";
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);

        when(cftTaskDatabaseService.findByTaskIdsByCaseId(caseId)).thenReturn(List.of(taskResourceCaseQueryBuilder1,
                taskResourceCaseQueryBuilder2));
        when(taskResourceCaseQueryBuilder1.getState()).thenReturn(UNASSIGNED);
        when(taskResourceCaseQueryBuilder2.getState()).thenReturn(UNASSIGNED);

        when(taskResourceCaseQueryBuilder1.getTaskId()).thenReturn("234");
        when(taskResourceCaseQueryBuilder2.getTaskId()).thenReturn("567");

        doNothing().when(deletedTasksDatabaseService).addProcessedTasks("123");

        final DeleteTasksResponse deleteTasksResponse = taskDeletionService.deleteTasksByCaseId(caseId);

        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getCaseTasksFound()).isEqualTo(2);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getDeletedCaseTasks()).isEqualTo(0);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getFailedCaseTasks()).isEqualTo(0);
    }

    @Test
    void shouldReturnDeletedCanceledAndFailedResponse() {
        final String caseId = "123";
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder1 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder2 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder3 = mock(TaskResourceCaseQueryBuilder.class);
        final TaskResourceCaseQueryBuilder taskResourceCaseQueryBuilder4 = mock(TaskResourceCaseQueryBuilder.class);

        when(cftTaskDatabaseService.findByTaskIdsByCaseId(caseId)).thenReturn(List.of(taskResourceCaseQueryBuilder1,
                taskResourceCaseQueryBuilder2, taskResourceCaseQueryBuilder3,taskResourceCaseQueryBuilder4));

        when(taskResourceCaseQueryBuilder1.getState()).thenReturn(TERMINATED);
        when(taskResourceCaseQueryBuilder2.getState()).thenReturn(TERMINATED);
        when(taskResourceCaseQueryBuilder3.getState()).thenReturn(UNASSIGNED);
        when(taskResourceCaseQueryBuilder4.getState()).thenReturn(CANCELLED);

        when(taskResourceCaseQueryBuilder1.getTaskId()).thenReturn("234");
        when(taskResourceCaseQueryBuilder2.getTaskId()).thenReturn("567");
        when(taskResourceCaseQueryBuilder3.getTaskId()).thenReturn("765");

        doNothing().when(cftTaskDatabaseService).deleteTask("234");
        doNothing().when(deletedTasksDatabaseService).addFailedToDeleteTasks("123");
        doThrow(RuntimeException.class).when(cftTaskDatabaseService).deleteTask("567");

        final DeleteTasksResponse deleteTasksResponse = taskDeletionService.deleteTasksByCaseId(caseId);

        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getCaseTasksFound()).isEqualTo(4);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getDeletedCaseTasks()).isEqualTo(1);
        assertThat(deleteTasksResponse.getCaseTasksDeletionResults().getFailedCaseTasks()).isEqualTo(1);
    }
}