package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaCftTaskStateUpdateException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singleton;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CFT_TASK_STATE;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.DataflowAnomalyAnalysis", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.GodClass", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.CyclomaticComplexity", "PMD.PreserveStackTrace"
})
public class CamundaTaskUpdateService {

    public static final String THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH = "There was a problem performing the search";
    public static final String THERE_WAS_A_PROBLEM_RETRIEVING_TASK_COUNT = "There was a problem retrieving task count";

    private static final String ESCALATION_CODE = "wa-esc-cancellation";

    private final CamundaServiceApi camundaServiceApi;

    @Autowired
    public CamundaTaskUpdateService(CamundaServiceApi camundaServiceApi) {
        this.camundaServiceApi = camundaServiceApi;
    }

    public List<CamundaTask> searchTaskWithCriteria(String query,
                                                    int firstResult,
                                                    int maxResults,
                                                    String authorisation) {

        try {
            return camundaServiceApi.searchWithCriteriaByQuery(
                authorisation,
                firstResult,
                maxResults,
                query
            );

        } catch (FeignException exp) {
            log.error(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH);
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH, exp);
        }
    }

    public List<HistoricCamundaTask> searchTaskHistoryWithCriteria(String query,
                                                                   String firstResult,
                                                                   String maxResults,
                                                                   String authorisation) {

        try {
            return camundaServiceApi.getTasksFromHistory(
                authorisation,
                firstResult,
                maxResults,
                query
            );

        } catch (FeignException exp) {
            log.error(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH);
            throw new ServerErrorException(THERE_WAS_A_PROBLEM_PERFORMING_THE_SEARCH, exp);
        }
    }

    public void deleteCftTaskState(String taskId, String authorisation) {

        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        try {
            List<HistoryVariableInstance> results = camundaServiceApi.searchHistory(
                authorisation,
                body
            );

            Optional<HistoryVariableInstance> maybeCftTaskState = results.stream()
                .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
                .findFirst();

            maybeCftTaskState.ifPresent(
                historyVariableInstance -> camundaServiceApi.deleteVariableFromHistory(
                    authorisation,
                    historyVariableInstance.getId()
                )
            );
        } catch (FeignException ex) {
            throw new ServerErrorException("There was a problem when deleting the historic cftTaskState", ex);
        }
    }

    public void updateCftTaskState(String taskId, TaskState taskState, String authorization) {
        try {
            updateCftTaskStateTo(taskId, taskState, authorization);
        } catch (CamundaCftTaskStateUpdateException ex) {
            throw new ServerErrorException("There was a problem when updating the cftTaskState", ex);
        }
    }

    private void updateCftTaskStateTo(String taskId, TaskState newState, String authorization) {
        Map<String, CamundaValue<String>> variable = Map.of(
            CFT_TASK_STATE.value(), CamundaValue.stringValue(newState.value())
        );

        AddLocalVariableRequest camundaLocalVariables = new AddLocalVariableRequest(variable);

        try {
            camundaServiceApi.addLocalVariablesToTask(authorization, taskId, camundaLocalVariables);
        } catch (FeignException ex) {
            log.error(
                "There was a problem updating task '{}', cft task state could not be updated to '{}'",
                taskId, newState
            );
            throw new CamundaCftTaskStateUpdateException(ex);
        }

        log.debug("Updated task '{}' with cft task state '{}'", taskId, newState.value());
    }
}
