package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;

@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionsDmnEvaluationResponse implements EvaluationResponse {
    private CamundaValue<String> name;
    private CamundaValue<String> value;
    private CamundaValue<String> authorisations;
    private CamundaValue<Integer> assignmentPriority;
    private CamundaValue<Boolean> autoAssignable;

    public PermissionsDmnEvaluationResponse() {
        //No-op constructor for deserialization
    }

    public PermissionsDmnEvaluationResponse(CamundaValue<String> name,
                                            CamundaValue<String> value,
                                            CamundaValue<String> authorisations,
                                            CamundaValue<Integer> assignmentPriority,
                                            CamundaValue<Boolean> autoAssignable) {
        this.name = name;
        this.value = value;
        this.authorisations = authorisations;
        this.assignmentPriority = assignmentPriority;
        this.autoAssignable = autoAssignable;
    }

    public CamundaValue<String> getName() {
        return name;
    }

    public CamundaValue<String> getValue() {
        return value;
    }

    public CamundaValue<String> getAuthorisations() {
        return authorisations;
    }

    public CamundaValue<Integer> getAssignmentPriority() {
        return assignmentPriority;
    }

    public CamundaValue<Boolean> getAutoAssignable() {
        return autoAssignable;
    }
}