package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class HistoricCamundaTask {

    private final String id;
    private final String deleteReason;

    public HistoricCamundaTask(@JsonProperty("id") String id,
                               @JsonProperty("deleteReason") String deleteReason) {
        this.id = id;
        this.deleteReason = deleteReason;
    }

    public String getId() {
        return id;
    }

    public String getDeleteReason() {
        return deleteReason;
    }
}
