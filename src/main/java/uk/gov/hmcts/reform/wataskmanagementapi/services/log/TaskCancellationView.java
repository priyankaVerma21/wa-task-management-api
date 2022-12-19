package uk.gov.hmcts.reform.wataskmanagementapi.services.log;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TaskCancellationView {

    private String taskId;
    private String state;
    private String caseId;
}
