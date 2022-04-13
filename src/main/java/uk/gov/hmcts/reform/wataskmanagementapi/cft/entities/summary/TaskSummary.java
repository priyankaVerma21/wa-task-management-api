package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.summary;

import lombok.EqualsAndHashCode;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;

import java.time.OffsetDateTime;

/*
A projection class for the TaskResource entity
 */
@EqualsAndHashCode
public class TaskSummary {

    private String taskId;
    private String taskName;
    private String taskType;
    private OffsetDateTime dueDateTime;
    private CFTTaskState state;

    public TaskSummary() {
    }

    public TaskSummary(String taskId,
                       String taskName,
                       String taskType,
                       OffsetDateTime dueDateTime,
                       CFTTaskState state) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskType = taskType;
        this.dueDateTime = dueDateTime;
        this.state = state;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskType() {
        return taskType;
    }

    public OffsetDateTime getDueDateTime() {
        return dueDateTime;
    }

    public CFTTaskState getState() {
        return state;
    }
}
