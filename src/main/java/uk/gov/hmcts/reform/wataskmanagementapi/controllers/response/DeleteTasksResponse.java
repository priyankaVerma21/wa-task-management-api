package uk.gov.hmcts.reform.wataskmanagementapi.controllers.response;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DeleteTasksResponse {

    private final CaseTasksDeletionResults caseTasksDeletionResults;

    public DeleteTasksResponse(final CaseTasksDeletionResults caseTasksDeletionResults) {
        this.caseTasksDeletionResults = caseTasksDeletionResults;
    }

    public CaseTasksDeletionResults getCaseTasksDeletionResults() {
        return caseTasksDeletionResults;
    }
}
