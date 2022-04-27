package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperation;

import java.util.List;

@Schema(
    name = "InitiateTaskRequest",
    description = "Allows specifying certain operations to initiate a task"
)
@EqualsAndHashCode
@ToString
public class InitiateTaskRequest {

    private final TaskOperation operation;
    private final List<TaskAttribute> taskAttributes;

    @JsonCreator
    public InitiateTaskRequest(TaskOperation operation,
                               List<TaskAttribute> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public TaskOperation getOperation() {
        return operation;
    }

    public List<TaskAttribute> getTaskAttributes() {
        return taskAttributes;
    }
}
