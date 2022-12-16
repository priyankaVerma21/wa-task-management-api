package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.CaseTasksDeletionResults;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.DeleteTasksResponse;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;


public final class DeleteTasksServiceHelper {

    private DeleteTasksServiceHelper() {
    }

    public static DeleteTasksResponse buildDeleteTasksResponse(
            final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders,
            final List<String> notTerminatedTaskIds,
            final List<String> terminatedTaskIds,
            final int failedToDeleteTasks,
            final int failedToCancelTasks) {
        return new DeleteTasksResponse(CaseTasksDeletionResults.builder()
                .caseTasksFound(taskResourceCaseQueryBuilders.size())
                .deletedCaseTasks(terminatedTaskIds.size() - failedToDeleteTasks)
                .cancelledCaseTasks(notTerminatedTaskIds.size() - failedToCancelTasks)
                .failedCaseTasks(failedToDeleteTasks + failedToCancelTasks)
                .build());
    }

    public static List<String> getTerminatedTaskIds(final List<TaskResourceCaseQueryBuilder> tasks) {
        return tasks.stream()
                .filter(task -> task.getState().equals(TERMINATED))
                .map(TaskResourceCaseQueryBuilder::getTaskId)
                .collect(toList());
    }

    public static List<String> getEligibleForCancellationTaskIds(final List<TaskResourceCaseQueryBuilder> tasks) {
        return tasks.stream()
                .filter(task -> !task.getState().equals(TERMINATED) && !task.getState().equals(CANCELLED))
                .map(TaskResourceCaseQueryBuilder::getTaskId)
                .collect(toList());
    }
}
