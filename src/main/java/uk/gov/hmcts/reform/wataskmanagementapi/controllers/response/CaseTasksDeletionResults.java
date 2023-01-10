package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Builder
@Schema(allowableValues = "CaseTasksDeletionResults")
public class CaseTasksDeletionResults {

    @Schema(
            required = true,
            description = "Total number of tasks for a case"
    )
    private int caseTasksFound;

    @Schema(
            required = true,
            description = "Deleted case tasks"
    )
    private int deletedCaseTasks;

    @Schema(
            required = true,
            description = "Eligible For Cancellation Tasks"
    )
    private int eligibleForCancellationTasks;

    @Schema(
            required = true,
            description = "Failed to delete case tasks"
    )
    private int failedCaseTasks;

//    public CaseTasksDeletionResults(final int caseTasksFound,
//                                    final int deletedCaseTasks,
//                                    final int failedCaseTasks) {
//        this.caseTasksFound = caseTasksFound;
//        this.deletedCaseTasks = deletedCaseTasks;
//        this.failedCaseTasks = failedCaseTasks;
//    }

    public int getCaseTasksFound() {
        return caseTasksFound;
    }

    public int getDeletedCaseTasks() {
        return deletedCaseTasks;
    }

    public int getFailedCaseTasks() {
        return failedCaseTasks;
    }

    public int getEligibleForCancellationTasks() {
        return eligibleForCancellationTasks;
    }
}