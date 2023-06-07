package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(name = "taskHistory")
@Table(name = "task_history")
public class TaskHistoryResource extends BaseTaskHistoryResource {

    @Override
    public String getTaskTitle() {
        return getTitle();
    }
}
