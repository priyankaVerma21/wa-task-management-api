package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.log.TaskCancellationLogService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.HOURS;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getEligibleForCancellationTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.DeleteTasksServiceHelper.getTerminatedTaskIds;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.CANCELLED_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.FAIL_TO_CANCEL_STATE;

@Service
@Slf4j
@EnableScheduling
public class TaskCancellationScheduler {

    private final TaskManagementService taskManagementService;

    private final TaskCancellationLogService taskCancellationLogService;

    private final DeletedTasksDatabaseService deletedTasksDatabaseService;

    private final CFTTaskDatabaseService cftTaskDatabaseService;

    @Autowired
    public TaskCancellationScheduler(final TaskManagementService taskManagementService,
                                     final TaskCancellationLogService taskCancellationLogService,
                                     final DeletedTasksDatabaseService deletedTasksDatabaseService,
                                     final CFTTaskDatabaseService cftTaskDatabaseService) {
        this.taskManagementService = taskManagementService;
        this.taskCancellationLogService = taskCancellationLogService;
        this.deletedTasksDatabaseService = deletedTasksDatabaseService;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    @Scheduled(fixedRate = 11, timeUnit = HOURS)
    private void processTasks() {

        final List<String> unprocessedCases = deletedTasksDatabaseService.getUnprocessedCases();

        unprocessedCases.forEach(caseId -> {
            final List<TaskResourceCaseQueryBuilder> taskResourceCaseQueryBuilders = cftTaskDatabaseService
                    .findByTaskIdsByCaseId(caseId);

            final List<String> eligibleForCancellationTaskIds = getEligibleForCancellationTaskIds(taskResourceCaseQueryBuilders);
            final List<String> terminatedTaskIds = getTerminatedTaskIds(taskResourceCaseQueryBuilders);

            final int failedToCancelTasks = cancelTasks(eligibleForCancellationTaskIds, caseId);
            final int failedToDeleteTasks = deleteTasks(terminatedTaskIds);

            updateCaseTasksDeletionState(failedToCancelTasks, failedToDeleteTasks, caseId);
        });

//        //Log task cancellations
        taskCancellationLogService.logCancellations(notTerminatedTaskIds);
    }

    private void updateCaseTasksDeletionState(final int failedToCancelTasks,
                                              final int failedToDeleteTasks,
                                              final String caseId) {

        final boolean isTasksSuccessfullyProcessedAndDeleted = failedToCancelTasks == 0 && failedToDeleteTasks == 0;

        if (isTasksSuccessfullyProcessedAndDeleted) {
            deletedTasksDatabaseService.addSuccessfullyDeletedTasks(caseId);
        }
    }


    private int cancelTasks(final List<String> notTerminatedTaskIds, final String caseId) {
        final AtomicInteger failedToCancelTasks = new AtomicInteger();
        notTerminatedTaskIds.forEach(task -> {
            try {
                taskManagementService.setTaskStateToCancelled(task);
                taskCancellationLogService.addTask(task, CANCELLED_STATE, caseId);
            } catch (final Exception exception) {
                taskCancellationLogService.addTask(task, FAIL_TO_CANCEL_STATE, caseId);
                failedToCancelTasks.incrementAndGet();
            }
        });
        return failedToCancelTasks.get();
    }

    private int deleteTasks(final List<String> terminatedTaskIds) {
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
