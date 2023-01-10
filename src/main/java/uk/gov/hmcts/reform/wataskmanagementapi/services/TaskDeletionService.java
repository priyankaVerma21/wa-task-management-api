package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.DeleteTasksResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.buildDeleteTasksResponse;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getEligibleForCancellationTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getTerminatedTaskIds;

@Slf4j
@Service
public class TaskDeletionService {
    private final CFTTaskDatabaseService cftTaskDatabaseService;

    private DeletedTasksDatabaseService deletedTasksDatabaseService;

    @Autowired
    public TaskDeletionService(final CFTTaskDatabaseService cftTaskDatabaseService,
                               final DeletedTasksDatabaseService deletedTasksDatabaseService) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.deletedTasksDatabaseService = deletedTasksDatabaseService;
    }

    public DeleteTasksResponse deleteTasksByCaseId(final String caseId) {
        final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders = cftTaskDatabaseService
                .findByTaskIdsByCaseId(caseId);

        final List<String> terminatedTaskIds = getTerminatedTaskIds(taskResourceCaseQueryBuilders);
        final List<String> notTerminatedTaskIds = getEligibleForCancellationTaskIds(taskResourceCaseQueryBuilders);

        //Delete TERMINATED tasks
        final int failedToDeleteTasks = deleteCaseTasks(terminatedTaskIds);

        deletedTasksDatabaseService.addProcessedTasks(caseId);

        return buildDeleteTasksResponse(taskResourceCaseQueryBuilders, notTerminatedTaskIds, terminatedTaskIds,
                failedToDeleteTasks);
    }

    private int deleteCaseTasks(final List<String> terminatedTaskIds) {
        final AtomicInteger failedToDeleteTasks = new AtomicInteger();
        terminatedTaskIds.forEach(id -> {
            try {
                cftTaskDatabaseService.deleteTask(id);
            } catch (final Exception exception) {
                failedToDeleteTasks.incrementAndGet();
            }
        });
        return failedToDeleteTasks.get();
    }
}