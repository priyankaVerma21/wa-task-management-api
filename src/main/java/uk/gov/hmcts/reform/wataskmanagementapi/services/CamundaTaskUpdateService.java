package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.actions.CamundaCftTaskStateUpdateException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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

    public CamundaVariableInstance getProcessInstanceIdFromTask(String taskId) {
        try {
            return getProcessInstanceId(taskId);
        } catch (CamundaCftTaskStateUpdateException ex) {
            throw new ServerErrorException("There was a problem when updating the cftTaskState", ex);
        }
    }

    public List<CamundaTask> retrieveTasksToFix(String serviceAuthorisation,
                                                int firstResult, int maxResults, String body) {
        try {
            return camundaServiceApi.retrieveTasks(serviceAuthorisation, firstResult, maxResults, body);
        } catch (FeignException ex) {
            log.error("There was an error while getting process instance ids : {}", body);
            throw new CamundaCftTaskStateUpdateException(ex);
        }
    }

    public HistoricCamundaProcess getProcessInstanceDetails(String serviceAuthorisation,
                                                            String processInstanceId) {
        try {
            return camundaServiceApi.retrieveProcessDetails(serviceAuthorisation, processInstanceId);
        } catch (FeignException ex) {
            log.error("There was an error while getting process instance ids : {}", processInstanceId);
            throw new CamundaCftTaskStateUpdateException(ex);
        }
    }

    public Map<String, CamundaVariable> getTaskVariables(String serviceAuthorisation, String taskId) {

        try {
            return camundaServiceApi.getVariables(serviceAuthorisation, taskId);
        } catch (FeignException ex) {
            log.error("There was an error while getting process instance id for task : {}", taskId);
            throw new CamundaCftTaskStateUpdateException(ex);
        }

    }

    private CamundaVariableInstance getProcessInstanceId(String taskId) {

        try {
            return camundaServiceApi.getProcessInstanceId(taskId);
        } catch (FeignException ex) {
            log.error("There was an error while getting process instance id for task : {}", taskId);
            throw new CamundaCftTaskStateUpdateException(ex);
        }

    }

    public void cancelProcess(String processInstanceId) {
        try {
            cancelProcessInstance(processInstanceId);
        } catch (CamundaCftTaskStateUpdateException ex) {
            throw new ServerErrorException("There was a problem when cancelling process", ex);
        }
    }

    private void cancelProcessInstance(String processInstanceId) {

        try {
            String[] processInstanceIds = new String[]{processInstanceId};
            Map<String, Object> body = Map.of(
                "deleteReason", "RWA-1589-to clean up data",
                "processInstanceIds", processInstanceIds
            );
            camundaServiceApi.deleteProcess(body);
        } catch (FeignException ex) {
            log.error("There was an error while cancelling process. processInstanceId : {}", processInstanceId);
            throw new CamundaCftTaskStateUpdateException(ex);
        }

        log.debug("process cancelled successfully. processInstanceId : '{}'", processInstanceId);
    }

    public boolean isCftTaskStatePendingTerminationInCamunda(String serviceAuthorisation, String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        AtomicBoolean isPendingTermination = new AtomicBoolean(false);

        //Check if the task has already been deleted or pending termination
        List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(serviceAuthorisation, body);

        if (result == null || result.isEmpty()) {
            log.info("{}  cftTaskState doesn't exist in Camunda", taskId);
            return isPendingTermination.get();
        }

        Optional<HistoryVariableInstance> historyVariableInstance = result.stream()
            .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
            .findFirst();

        historyVariableInstance.ifPresent(variable -> {
            log.info("{} cftTaskStateInCamundaHistory: {}", taskId, variable.getValue());

            if ("pendingTermination".equalsIgnoreCase(variable.getValue())) {
                isPendingTermination.set(true);
            }

        });

        return isPendingTermination.get();

    }

    public CamundaTask getTaskIdByProcessInstanceId(String serviceAuthorisation, String processInstanceId) {

        try {
            List<CamundaTask> camundaTaskList = camundaServiceApi.getTaskIdByProcessInstanceId(serviceAuthorisation, processInstanceId);
            if (camundaTaskList != null && camundaTaskList.size() > 0) {
                return camundaTaskList.get(0);
            }
            log.info("An error occurred getting task detail by processId: {} - camundaTaskList is null or empty", processInstanceId);
            throw new CamundaCftTaskStateUpdateException(new RuntimeException("camundaTaskList is null or empty"));
        } catch (Exception e) {
            log.info("An error occurred getting task detail by processId: {} - exception: {}", processInstanceId, e);
            throw new CamundaCftTaskStateUpdateException(e);
        }

    }

    public boolean checkCftTaskStateExists(String serviceAuthorisation, String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(serviceAuthorisation, body);

        if (result == null || result.isEmpty()) {
            log.info("{}  cftTaskState doesn't exist in Camunda", taskId);
            return false;
        }

        return true;

    }

    public boolean isCftTaskStateUnconfigured(String serviceAuthorisation, String taskId) {
        AtomicBoolean isCftTaskStateUnconfigured = new AtomicBoolean(false);
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(serviceAuthorisation, body);

        if (result == null || result.isEmpty()) {
            log.info("{}  cftTaskState doesn't exist in Camunda", taskId);
            return isCftTaskStateUnconfigured.get();
        }

        Optional<HistoryVariableInstance> historyVariableInstance = result.stream()
            .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
            .findFirst();

        historyVariableInstance.ifPresent(variable -> {
            log.info("{} cftTaskStateInCamundaHistory: {}", taskId, variable.getValue());

            if ("unconfigured".equalsIgnoreCase(variable.getValue())) {
                isCftTaskStateUnconfigured.set(true);
            }

        });

        return isCftTaskStateUnconfigured.get();

    }

    public boolean isCftTaskStateExistInCamunda(String serviceAuthorisation, String taskId) {
        Map<String, Object> body = Map.of(
            "variableName", CFT_TASK_STATE.value(),
            "taskIdIn", singleton(taskId)
        );

        AtomicBoolean isCftTaskStateExist = new AtomicBoolean(false);

        //Check if the task has already been deleted or pending termination
        List<HistoryVariableInstance> result = camundaServiceApi.searchHistory(serviceAuthorisation, body);

        if (result == null || result.isEmpty()) {
            log.info("{}  cftTaskState doesn't exist in Camunda", taskId);
            return isCftTaskStateExist.get();
        }

        Optional<HistoryVariableInstance> historyVariableInstance = result.stream()
            .filter(r -> r.getName().equals(CFT_TASK_STATE.value()))
            .findFirst();

        historyVariableInstance.ifPresent(variable -> {
            log.info("{} cftTaskStateInCamundaHistory: {}", taskId, variable.getValue());

            if ("pendingTermination".equalsIgnoreCase(variable.getValue())) {
                isCftTaskStateExist.set(true);
            }

        });

        return isCftTaskStateExist.get();

    }

}
