package uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums;

public enum ErrorMessages {

    TASK_UNCLAIM_UNABLE_TO_UPDATE_STATE(
        "Task unclaim failed. "
        + "Unable to update task state to unassigned."),

    TASK_UNCLAIM_UNABLE_TO_UNCLAIM(
        "Task unclaim partially succeeded. "
        + "The Task state was updated to unassigned, but the Task could not be unclaimed."),

    TASK_COMPLETE_UNABLE_TO_UPDATE_STATE(
        "Task complete failed. "
        + "Unable to update task state to completed."),

    TASK_COMPLETE_UNABLE_TO_COMPLETE(
        "Task complete partially succeeded. "
        + "The Task state was updated to completed, but the Task could not be completed."),

    TASK_CLAIM_UNABLE_TO_UPDATE_STATE(
        "Task claim failed. "
        + "Unable to update task state to assigned."),

    TASK_CLAIM_UNABLE_TO_CLAIM(
        "Task claim partially succeeded. "
        + "The Task state was updated to assigned, but the Task could not be claimed."),

    TASK_CANCEL_UNABLE_TO_CANCEL(
        "Unable to cancel the task."),

    TASK_ASSIGN_UNABLE_TO_UPDATE_STATE(
        "Task assign failed. "
        + "Unable to update task state to assigned."),

    TASK_ASSIGN_UNABLE_TO_ASSIGN(
        "Task assign partially succeeded. "
        + "The Task state was updated to assigned, but the Task could not be assigned."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_ASSIGN(
        "Unable to assign the Task to the current user."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_UPDATE_STATE(
        "Task assign and complete partially succeeded. "
        + "The Task was assigned to the user making the request but the Task could not be completed."),

    TASK_ASSIGN_AND_COMPLETE_UNABLE_TO_COMPLETE(
        "Task assign and complete partially succeeded. "
        + "The Task was assigned to the user making the request, the task state was also updated to completed, "
        + "but he Task could not be completed.");

    private final String detail;

    ErrorMessages(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }

}