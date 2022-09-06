package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaTaskUpdateService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
public class UpdateCamundaTaskVariable extends SpringBootFunctionalBaseTest {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public static final String CAMUNDA_DATE_REQUEST_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CAMUNDA_DATE_REQUEST_PATTERN);

    AtomicInteger processedTaskCount = new AtomicInteger();
    List<String> failedTasks = new ArrayList<>();
    List<String> successfulTasks = new ArrayList<>();

    @Autowired
    CamundaTaskUpdateService camundaService;

    @Before
    public void before() {
        //
    }

    @DisplayName("RWA_1589-after cancel task job check cftTaskState is gone")
    @Test
    public void check_cft_task_state_is_gone() {

        String serviceAuthorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";

        String input = getResource("local_assigned_task_for_migration.csv");
        String[] split = input.split(",\n");

        AtomicInteger processedTaskCount = new AtomicInteger();
        List<String> failedTasks = new ArrayList<>();
        List<String> successfulTasks = new ArrayList<>();

        Arrays.asList(split).forEach(taskId ->
        {
            taskId = taskId.trim();

            try {

                boolean isCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(serviceAuthorisation, taskId);
                if (isCftTaskStateExist) {
                    String logMessage = String.format("task id: %s - cftTaskState exist", taskId);
                    throw new RuntimeException(logMessage);
                }

                successfulTasks.add(taskId);
            } catch (Exception e) {
                log.error(e.getMessage());
                failedTasks.add(String.format("task id: %s |Error Message : %s", taskId, e.getMessage()));
                colouredPrintLine(ANSI_RED, "❌ " + taskId);
            }


            processedTaskCount.getAndIncrement();
            colouredPrintLine(ANSI_YELLOW, "count: " + processedTaskCount.get());
            if (processedTaskCount.get() % 10 == 0) {
                sleep(1);
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                colouredPrintLine(ANSI_CYAN, "|------------------------❄️❄️SHORT BREAK❄️❄️----------------------|");
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                sleep(4);
            }

        });


        successfulTasks.forEach(t -> log.info("🍻 Successful--> {}", t));
        failedTasks.forEach(t -> log.error("🚨 Failed--> {}", t));
        log.info("successfulTasks: {} - failedTasks: {}", successfulTasks.size(), failedTasks.size());

    }

    @DisplayName("RWA_1589-cancel tasks")
    @Test
    public void cancel_camunda_task_while_reading_from_a_file() {
        //todo: suspend termination job during this process running (cft-flux-config)

        String serviceAuthorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";

        String input = getResource("local_assigned_task_for_migration.csv");
        String[] split = input.split(",\n");

        LocalDateTime migrationLocalDate = LocalDateTime.of(
            2022, 07, 02,
            23, 59, 0);
        ZoneId zoneId = ZoneId.of("Europe/London");
        ZonedDateTime migrationDate = ZonedDateTime.of(migrationLocalDate, zoneId);

        AtomicInteger processedTaskCount = new AtomicInteger();
        List<String> failedTasks = new ArrayList<>();
        List<String> successfulTasks = new ArrayList<>();

        Arrays.asList(split).forEach(taskId ->
        {
            taskId = taskId.trim();

            try {
                log.info("task id: {} getting task variables", taskId);
                Map<String, CamundaVariable> variables = camundaService.getTaskVariables(serviceAuthorisation, taskId);

                String taskState = variables.get("taskState").getValue().toString();
                log.info("task id: {} checking taskState. {}", taskId, taskState);
                if (!"unconfigured".equalsIgnoreCase(taskState)) {
                    String logMessage = String.format("task id: %s - task state is not 'unconfigured'. taskState: %s",
                        taskId, taskState);
                    throw new RuntimeException(logMessage);
                }

                log.info("task id: {} getting process variables ", taskId);
                CamundaVariableInstance camundaVariableInstance = camundaService.getProcessInstanceIdFromTask(taskId);

                log.info("task id: {} checking create date", taskId);
                String created = camundaVariableInstance.getCreated();
                if (ZonedDateTime.parse(created, formatter).isAfter(migrationDate)) {
                    String logMessage =
                        String.format("task id: %s task created date after migration. migrationDate: %s - created: %s",
                            taskId, migrationDate, created);

                    throw new RuntimeException(logMessage);
                }

                String processInstanceId = camundaVariableInstance.getProcessInstanceId();
                log.info("task id: {} cancelling process instance processInstanceId: {}", taskId, processInstanceId);
                camundaService.cancelProcess(processInstanceId);

                log.info("task id: {} checking cftTaskState is pendingTermination in Camunda", taskId);
                sleep(2);
                checkCftTaskStateIsPendingTermination(serviceAuthorisation, taskId, 0);

                log.info("task id: {} removing 'cftTaskState' from task. processInstanceId: {}",
                    taskId, processInstanceId);
                camundaService.deleteCftTaskState(taskId, serviceAuthorisation);


                log.info("task id: {} cancellation process successfully completed. processInstanceId: {}",
                    taskId, processInstanceId);

                successfulTasks.add(String.format("taskId: %s processInstanceId: %s", taskId, processInstanceId));
                colouredPrintLine(ANSI_GREEN, "✅ " + taskId);

            } catch (Exception e) {
                log.error(e.getMessage());
                failedTasks.add(String.format("task id: %s |Error Message : %s", taskId, e.getMessage()));
                colouredPrintLine(ANSI_RED, "❌ " + taskId);
            }

            processedTaskCount.getAndIncrement();
            colouredPrintLine(ANSI_YELLOW, "count: " + processedTaskCount.get());
            if (processedTaskCount.get() % 10 == 0) {
                sleep(1);
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                colouredPrintLine(ANSI_CYAN, "|------------------------❄️❄️SHORT BREAK❄️❄️----------------------|");
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                sleep(4);
            }
        });


        successfulTasks.forEach(t -> log.info("🍻 Successful--> {}", t));
        failedTasks.forEach(t -> log.error("🚨 Failed--> {}", t));
        log.info("successfulTasks: {} - failedTasks: {}", successfulTasks.size(), failedTasks.size());

    }


    @DisplayName("RWA_1754-put cftTaskState variable into a task")
    @Test
    public void put_cf_task_state_into_task() {
        //todo: change initiation camunda time flag for initiate updated tasks

        String serviceAuthorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";

        int firstResult = 0;
        int maxResults = 10;

        String query = getResource("camunda-search-process-query.json");

        while (true) {
            log.info("Getting task ids from camunda");
            List<CamundaTask> camundaTasks = camundaService.retrieveTasksToFix(
                serviceAuthorisation,
                firstResult,
                maxResults,
                query);

            if (camundaTasks == null || camundaTasks.size() < 1) {
                break;
            }

            putCfTaskStateIntoTask(serviceAuthorisation, camundaTasks);

            firstResult += maxResults;
        }

        successfulTasks.forEach(t -> log.info("🍻 Successful--> {}", t));
        failedTasks.forEach(t -> log.error("🚨 Failed--> {}", t));
        log.info("successfulTasks: {} - failedTasks: {}", successfulTasks.size(), failedTasks.size());

    }

    public void putCfTaskStateIntoTask(String serviceAuthorisation,
                                       List<CamundaTask> camundaTaskList) {


        AtomicReference<String> taskId = new AtomicReference<>();
        AtomicReference<String> processId = new AtomicReference<>();

        camundaTaskList.forEach(task -> {
            try {
                taskId.set(task.getId());

                log.info("task id: {} getting process variables ", taskId);
                CamundaVariableInstance camundaVariableInstance = camundaService.getProcessInstanceIdFromTask(taskId.get());
                processId.set(camundaVariableInstance.getProcessInstanceId());

                log.info("processId: {} getting process details", processId.get());
                HistoricCamundaProcess historicCamundaProcess = camundaService.getProcessInstanceDetails(serviceAuthorisation, processId.get());

                if (!"ACTIVE".equals(historicCamundaProcess.getState())) {
                    String logMessage = String.format("task id: %s - process not active. state: %s",
                        taskId, historicCamundaProcess.getState());
                    throw new RuntimeException(logMessage);
                }

                log.info("task id: {} getting task variables", taskId);
                Map<String, CamundaVariable> variables = camundaService.getTaskVariables(serviceAuthorisation, taskId.get());

                String taskState = variables.get("taskState").getValue().toString();
                log.info("task id: {} checking taskState. {}", taskId, taskState);

                log.info("task id: {} checking cftTaskState exists or not.", taskId);
                boolean isCftTaskStateExists = camundaService.checkCftTaskStateExists(serviceAuthorisation, taskId.get());

                if (isCftTaskStateExists) {
                    String logMessage = String.format("task id: %s - cftTaskState exists in camunda.", taskId);
                    throw new RuntimeException(logMessage);
                }

                if (!"unconfigured".equalsIgnoreCase(taskState)) {
                    String logMessage = String.format("task id: %s - task state is not 'unconfigured'. taskState: %s",
                        taskId, taskState);
                    throw new RuntimeException(logMessage);
                }

                log.info("task id: {} trying to put cftTaskState into camunda", taskId);
                camundaService.updateCftTaskState(taskId.get(), TaskState.UNCONFIGURED, serviceAuthorisation);

                sleep(2);
                log.info("task id: {} checking cftTaskState is 'UNCONFIGURED' in camunda", taskId);
                boolean isCftTaskStateUnconfigured = camundaService.isCftTaskStateUnconfigured(serviceAuthorisation, taskId.get());
                if (!isCftTaskStateUnconfigured) {
                    String logMessage = String.format("task id: %s - cftTaskState is not 'UNCONFIGURED'", taskId);
                    throw new RuntimeException(logMessage);
                }

                log.info("task id: {} adding cftTaskState='UNCONFIGURED' process successfully completed. processInstanceId: {}",
                    taskId.get(), processId.get());

                successfulTasks.add(String.format("taskId: %s processInstanceId: %s", taskId.get(), processId.get()));
                colouredPrintLine(ANSI_GREEN, "✅ " + processId.get());

            } catch (Exception e) {
                log.error(e.getMessage());
                failedTasks.add(String.format("task id: %s |Error Message : %s", taskId.get(), e.getMessage()));
                colouredPrintLine(ANSI_RED, "❌ " + taskId.get());
            }

            processedTaskCount.getAndIncrement();
            colouredPrintLine(ANSI_YELLOW, "count: " + processedTaskCount.get());
            if (processedTaskCount.get() % 10 == 0) {
                sleep(1);
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                colouredPrintLine(ANSI_CYAN, "|------------------------❄️❄️SHORT BREAK❄️❄️----------------------|");
                colouredPrintLine(ANSI_CYAN, "|-----------------------------------------------------------------|");
                sleep(4);
            }
        });


    }

    private void checkCftTaskStateIsPendingTermination(String serviceAuthorisation, String taskId, int tryCount) {

        if (tryCount == 5) {
            sleep(5);
        }

        if (tryCount >= 10) {
            String logMessage =
                String.format("task id: %s failed to many attempts isCftTaskStatePendingTerminationInCamunda. tryCount: %s ", taskId, tryCount);
            throw new RuntimeException(logMessage);
        }
        boolean isCftTaskStatePendingTerminationInCamunda = camundaService.isCftTaskStatePendingTerminationInCamunda(serviceAuthorisation, taskId);

        tryCount++;

        if (!isCftTaskStatePendingTerminationInCamunda) {
            checkCftTaskStateIsPendingTermination(serviceAuthorisation, taskId, tryCount);
            sleep(2);
        }

    }

    private void sleep(int second) {
        try {
            Thread.sleep(second * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void update_camunda_task_variable_while_reading_from_a_file() {
        String authorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";
        String input = getResource("assigned_task_for_migration.csv");
        String[] split = input.split(",\n");


        List<String> failedTasks = new ArrayList<>();
        Arrays.asList(split).forEach(taskId ->
        {
            log.info("updating cft task state of task id " + taskId.trim());
            try {
                //camundaService.updateCftTaskState(
                //    taskId.trim(),
                //    TaskState.ASSIGNED,
                //    authorisation
                //);
            } catch (Exception ex) {
                failedTasks.add(taskId);
            }
        });


        log.info("failed tasks are ");
        failedTasks.forEach(t -> log.info("failed task id is {}", t));
    }

    @Test
    public void update_camunda_task_variable() {
        String authorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";
        String query = getResource("camunda-task-assigned-query.json");
        ZonedDateTime endTime = ZonedDateTime.now().minusYears(1);
        ZonedDateTime startTime = ZonedDateTime.now();
        String finishedAfter = endTime.format(formatter);
        String finishedBefore = startTime.format(formatter);
        query = query
            .replace("\"finishedAfter\": \"*\",", "\"finishedAfter\": \""
                                                  + finishedAfter + "\",");
        query = query
            .replace("\"finishedBefore\": \"*\",", "\"finishedBefore\": \""
                                                   + finishedBefore + "\",");

        List<String> failedTasks = new ArrayList<>();
        for (int firstResult = 0; firstResult < 1; firstResult += 100) {
            log.info("firstResult " + firstResult);
            String firstResultString = Integer.toString(firstResult);
            log.info("processing for first result " + firstResultString);

            List<CamundaTask> historicCamundaTasks = camundaService.searchTaskWithCriteria(
                query,
                0,
                100,
                authorisation
            );
            historicCamundaTasks
                .forEach(task ->
                {
                    String taskId = task.getId();
                    log.info("updating cft task state of task id " + taskId);
                    try {
                        camundaService.updateCftTaskState(
                            taskId,
                            TaskState.UNCONFIGURED,
                            authorisation
                        );
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        failedTasks.add(taskId);
                    }
                });


            log.info("failed tasks are ");
            failedTasks.forEach(t -> log.info("failed task id is {}", t));
        }
    }

    @Test
    public void generate_query_for_cft_task_update() {
        String authorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";
        String query = getResource("camunda-task-assigned-query.json");
        for (int firstResult = 0; firstResult < 2461; firstResult += 100) {
            List<CamundaTask> historicCamundaTasks = camundaService.searchTaskWithCriteria(
                query,
                firstResult,
                100,
                authorisation
            );
            AtomicReference<List<String>> taskIds = new AtomicReference<>(new ArrayList<>());
            AtomicReference<String> assignee = new AtomicReference<>();
            historicCamundaTasks
                .forEach(task ->
                {
                    if (assignee.get() == null || task.getAssignee().equals(assignee.get())) {
                        taskIds.get().add(task.getId());
                        //for first time
                        assignee.set(task.getAssignee());

                    } else {
                        StringBuilder output = new StringBuilder("update tasks set assignee = '");
                        output.append(assignee.get());
                        output.append("', state = 'ASSIGNED'");
                        output.append(" where task_id in (");
                        taskIds.get().forEach(id -> {
                            output.append("'");
                            output.append(id);
                            output.append("',");
                        });
                        output.deleteCharAt(output.length() - 1);
                        output.append(");");
                        log.info(output.toString());
                        taskIds.set(new ArrayList<>());
                        assignee.set(null);

                    }


                });
        }
    }

    private void writeToFile(BufferedWriter info, StringBuilder output) {
        try {
            info.write(output.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void update_camunda_history_task_variable() {
        String authorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";
        String query = getResource("camunda-historic-task-pending-termination-query.json");
        ZonedDateTime endTime = ZonedDateTime.now().minusYears(1);
        ZonedDateTime startTime = ZonedDateTime.now();
        String finishedAfter = endTime.format(formatter);
        String finishedBefore = startTime.format(formatter);
        query = query
            .replace("\"finishedAfter\": \"*\",", "\"finishedAfter\": \""
                                                  + finishedAfter + "\",");
        query = query
            .replace("\"finishedBefore\": \"*\",", "\"finishedBefore\": \""
                                                   + finishedBefore + "\",");

        List<String> failedTasks = new ArrayList<>();
        for (int firstResult = 0; firstResult < 100; firstResult += 100) {
            log.info("firstResult " + firstResult);
            String firstResultString = Integer.toString(firstResult);
            log.info("processing for first result " + firstResultString);

            List<HistoricCamundaTask> historicCamundaTasks = camundaService.searchTaskHistoryWithCriteria(
                query,
                "0",
                "100",
                authorisation
            );
            historicCamundaTasks.forEach(task ->
            {
                String taskId = task.getId();
                log.info("deleting cft task state of task id " + taskId);
                try {
                    camundaService.deleteCftTaskState(taskId, authorisation);
                } catch (Exception ex) {
                    failedTasks.add(taskId);
                }
            });


            log.info("failed tasks are ");
            failedTasks.forEach(t -> log.info("failed task id is {}", t));
        }
    }

    public static String getResource(String fileName) {
        try (var is = new ClassPathResource(fileName).getInputStream()) {
            return FileCopyUtils.copyToString(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }


    @Test
    public void get_task_id() {
        try {
            Path path = Paths.get("local_assigned_task_for_migration.csv");
            List<Map<String, Object>> data = new ObjectMapper().readValue(mockJson(),
                new TypeReference<List<Map<String, Object>>>() {
                });

            List<String> taskIds = data.stream().map(x -> x.get("id").toString()).collect(Collectors.toList());

            String str = String.join(",\n", taskIds) + ",\n";
            byte[] bytes = str.getBytes();
            Files.write(path, bytes, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String mockJson() {
        return "[{\"id\":\"797fdf86-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"78c03962-f233-11ec-975e-8609d135d703\",\"executionId\":\"797fdf84-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:797fdf85-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:59:03.734+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"78c03962-f233-11ec-975e-8609d135d703\"},{\"id\":\"79373dbf-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"784feac8-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"79373dbd-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:79373dbe-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:59:03.258+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"784feac8-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"788e0561-f233-11ec-8670-8ab7eec3f591\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"780833ff-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"788e055f-f233-11ec-8670-8ab7eec3f591\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:788e0560-f233-11ec-8670-8ab7eec3f591\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:59:02.148+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"780833ff-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"7871cb64-f233-11ec-aaed-6e6f655125b8\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"780e27ae-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"7871cb62-f233-11ec-aaed-6e6f655125b8\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:7871cb63-f233-11ec-aaed-6e6f655125b8\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:59:01.965+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"780e27ae-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"7289f3ae-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"71f0e880-f233-11ec-aaed-6e6f655125b8\",\"executionId\":\"7289f3ac-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:7289f3ad-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:52.056+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"71f0e880-f233-11ec-aaed-6e6f655125b8\"},{\"id\":\"6c7e4163-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"6c1bae69-f233-11ec-b9ff-b6fe72bbde0d\",\"executionId\":\"6c7e4161-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:6c7e4162-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:41.913+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"6c1bae69-f233-11ec-b9ff-b6fe72bbde0d\"},{\"id\":\"67911d35-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"6689e628-f233-11ec-8a73-5657c83ace6d\",\"executionId\":\"67911d33-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:67911d34-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:33.648+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"6689e628-f233-11ec-8a73-5657c83ace6d\"},{\"id\":\"6769981b-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"66841a56-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"67699819-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:6769981a-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:33.389+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"66841a56-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"613ca1c6-f233-11ec-b9ff-b6fe72bbde0d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"604d86a9-f233-11ec-b9ff-b6fe72bbde0d\",\"executionId\":\"613ca1c4-f233-11ec-b9ff-b6fe72bbde0d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:613ca1c5-f233-11ec-b9ff-b6fe72bbde0d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:23.028+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"604d86a9-f233-11ec-b9ff-b6fe72bbde0d\"},{\"id\":\"5bad2401-f233-11ec-b9ff-b6fe72bbde0d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"5ab2bdd0-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"5bad23ff-f233-11ec-b9ff-b6fe72bbde0d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:5bad2400-f233-11ec-b9ff-b6fe72bbde0d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:13.699+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"5ab2bdd0-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"5b3422a2-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"5a2c76ab-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"5b3422a0-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:5b3422a1-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:12.999+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"5a2c76ab-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"5b3470c5-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"5a2a05c8-f233-11ec-8a73-5657c83ace6d\",\"executionId\":\"5b3470c3-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:5b3470c4-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:12.908+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"5a2a05c8-f233-11ec-8a73-5657c83ace6d\"},{\"id\":\"5568ad24-f233-11ec-8670-8ab7eec3f591\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"5476d35d-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"55688612-f233-11ec-8670-8ab7eec3f591\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:55688613-f233-11ec-8670-8ab7eec3f591\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:03.184+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"5476d35d-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"5524517e-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"544d5260-f233-11ec-b9ff-b6fe72bbde0d\",\"executionId\":\"55242a6c-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:55242a6d-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:02.736+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"544d5260-f233-11ec-b9ff-b6fe72bbde0d\"},{\"id\":\"54e68528-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"544d2b2c-f233-11ec-9da8-665b1b4af445\",\"executionId\":\"54e68526-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:54e68527-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:58:02.332+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"544d2b2c-f233-11ec-9da8-665b1b4af445\"},{\"id\":\"522a24b0-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"513ed95a-f233-11ec-9da8-665b1b4af445\",\"executionId\":\"522a24ac-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:522a24ad-f233-11ec-849a-6e00b5c8b816\",\"name\":\"task name\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:57.741+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-07-02T13:57:56.176+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"513ed95a-f233-11ec-9da8-665b1b4af445\"},{\"id\":\"4ff3e364-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"4e62bbe8-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"4ff3e362-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:4ff3e363-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:54.030+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"4e62bbe8-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"4fe89925-f233-11ec-aaed-6e6f655125b8\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"4e6e2dc9-f233-11ec-975e-8609d135d703\",\"executionId\":\"4fe8991f-f233-11ec-aaed-6e6f655125b8\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:4fe89921-f233-11ec-aaed-6e6f655125b8\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:53.957+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"4e6e2dc9-f233-11ec-975e-8609d135d703\"},{\"id\":\"4fe89927-f233-11ec-aaed-6e6f655125b8\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"4e9fec55-f233-11ec-aaed-6e6f655125b8\",\"executionId\":\"4fe89920-f233-11ec-aaed-6e6f655125b8\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:4fe89922-f233-11ec-aaed-6e6f655125b8\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:53.957+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"4e9fec55-f233-11ec-aaed-6e6f655125b8\"},{\"id\":\"4aed6bb3-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"49a3fba2-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"4aed6bb1-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:4aed6bb2-f233-11ec-849a-6e00b5c8b816\",\"name\":\"task name\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:45.599+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-07-02T13:57:43.414+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"49a3fba2-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"4a8a63c4-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"488a9bd2-f233-11ec-975e-8609d135d703\",\"executionId\":\"4a8a63c2-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:4a8a63c3-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:44.950+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"488a9bd2-f233-11ec-975e-8609d135d703\"},{\"id\":\"49eaa1c6-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"484d4490-f233-11ec-975e-8609d135d703\",\"executionId\":\"49eaa1c4-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:49eaa1c5-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:43.906+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"484d4490-f233-11ec-975e-8609d135d703\"},{\"id\":\"49e6f80b-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"488e4569-f233-11ec-b9ff-b6fe72bbde0d\",\"executionId\":\"49e6f809-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:49e6f80a-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:43.879+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"488e4569-f233-11ec-b9ff-b6fe72bbde0d\"},{\"id\":\"49a4bee8-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"484fb618-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"49a4bee6-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:49a4bee7-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:43.445+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"484fb618-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"430b7218-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"429d94bf-f233-11ec-b9ff-b6fe72bbde0d\",\"executionId\":\"430b4b06-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:430b4b07-f233-11ec-8a73-5657c83ace6d\",\"name\":\"task name\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:32.373+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-07-02T13:57:31.623+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"429d94bf-f233-11ec-b9ff-b6fe72bbde0d\"},{\"id\":\"3d7627cd-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3cc43cb0-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"3d7600bb-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3d7600bc-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:23.007+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3cc43cb0-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"3d7600ba-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3cb6cf2a-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"3d7600b8-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3d7600b9-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:23.006+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3cb6cf2a-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"3d4839c8-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3c7a142c-f233-11ec-975e-8609d135d703\",\"executionId\":\"3d4839c6-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3d4839c7-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:22.707+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3c7a142c-f233-11ec-975e-8609d135d703\"},{\"id\":\"3d252125-f233-11ec-b9ff-b6fe72bbde0d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3c7a6248-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"3d252123-f233-11ec-b9ff-b6fe72bbde0d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3d252124-f233-11ec-b9ff-b6fe72bbde0d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:22.477+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3c7a6248-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"3c133ba3-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3bb2300e-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"3c133ba1-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3c133ba2-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"task name\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:20.682+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-07-02T13:57:20.021+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3bb2300e-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"37c89a2c-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"36a94709-f233-11ec-aaed-6e6f655125b8\",\"executionId\":\"37c8731a-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:37c8731b-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:13.480+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"36a94709-f233-11ec-aaed-6e6f655125b8\"},{\"id\":\"37952dbf-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"36bc59e8-f233-11ec-8bf6-76f3730e8ff1\",\"executionId\":\"37952dbd-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:37952dbe-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:13.143+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"36bc59e8-f233-11ec-8bf6-76f3730e8ff1\"},{\"id\":\"378b42de-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"368321f5-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"378b42dc-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:378b42dd-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:13.078+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"368321f5-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"3106b650-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3080e4ee-f233-11ec-975e-8609d135d703\",\"executionId\":\"3106b64e-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3106b64f-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:02.144+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3080e4ee-f233-11ec-975e-8609d135d703\"},{\"id\":\"3106b64a-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"30b33ed6-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"31068f38-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:31068f39-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:02.143+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"30b33ed6-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"3106b64d-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"3066a5c4-f233-11ec-8a73-5657c83ace6d\",\"executionId\":\"3106b64b-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:3106b64c-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:57:02.143+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"3066a5c4-f233-11ec-8a73-5657c83ace6d\"},{\"id\":\"2b38d011-f233-11ec-8670-8ab7eec3f591\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"2ade0514-f233-11ec-aaed-6e6f655125b8\",\"executionId\":\"2b38d00f-f233-11ec-8670-8ab7eec3f591\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:2b38d010-f233-11ec-8670-8ab7eec3f591\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:52.405+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"2ade0514-f233-11ec-aaed-6e6f655125b8\"},{\"id\":\"2aea6148-f233-11ec-aaed-6e6f655125b8\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"2a7d6f09-f233-11ec-849a-6e00b5c8b816\",\"executionId\":\"2aea6146-f233-11ec-aaed-6e6f655125b8\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:2aea6147-f233-11ec-aaed-6e6f655125b8\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:51.893+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"2a7d6f09-f233-11ec-849a-6e00b5c8b816\"},{\"id\":\"2adc7ec3-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"2a7be822-f233-11ec-8bf6-76f3730e8ff1\",\"executionId\":\"2adc7ec1-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:2adc7ec2-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:51.801+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"2a7be822-f233-11ec-8bf6-76f3730e8ff1\"},{\"id\":\"26150bb1-f233-11ec-9da8-665b1b4af445\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"2513a0e8-f233-11ec-8bf6-76f3730e8ff1\",\"executionId\":\"26150baf-f233-11ec-9da8-665b1b4af445\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:26150bb0-f233-11ec-9da8-665b1b4af445\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:43.783+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"2513a0e8-f233-11ec-8bf6-76f3730e8ff1\"},{\"id\":\"25b86d15-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"24a0461d-f233-11ec-975e-8609d135d703\",\"executionId\":\"25b84603-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:25b84604-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:43.179+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"24a0461d-f233-11ec-975e-8609d135d703\"},{\"id\":\"2550f8b2-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"24c7f26f-f233-11ec-8a73-5657c83ace6d\",\"executionId\":\"2550f8b0-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:2550f8b1-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:42.497+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"24c7f26f-f233-11ec-8a73-5657c83ace6d\"},{\"id\":\"2550aa8f-f233-11ec-975e-8609d135d703\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"24b04b55-f233-11ec-9da8-665b1b4af445\",\"executionId\":\"2550aa8d-f233-11ec-975e-8609d135d703\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:2550aa8e-f233-11ec-975e-8609d135d703\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:42.495+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"24b04b55-f233-11ec-9da8-665b1b4af445\"},{\"id\":\"254c3d9b-f233-11ec-b9ff-b6fe72bbde0d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"24ccd489-f233-11ec-aaed-6e6f655125b8\",\"executionId\":\"254c3d99-f233-11ec-b9ff-b6fe72bbde0d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:254c3d9a-f233-11ec-b9ff-b6fe72bbde0d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:42.467+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"24ccd489-f233-11ec-aaed-6e6f655125b8\"},{\"id\":\"20c7a033-f233-11ec-849a-6e00b5c8b816\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"1f4ee2b8-f233-11ec-975e-8609d135d703\",\"executionId\":\"20c7a031-f233-11ec-849a-6e00b5c8b816\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:20c7a032-f233-11ec-849a-6e00b5c8b816\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:34.909+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"1f4ee2b8-f233-11ec-975e-8609d135d703\"},{\"id\":\"202d5b94-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"1f4d0d43-f233-11ec-8a73-5657c83ace6d\",\"executionId\":\"202d5b92-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:202d5b93-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:33.923+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"1f4d0d43-f233-11ec-8a73-5657c83ace6d\"},{\"id\":\"1ee28c70-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"1e8d4038-f233-11ec-975e-8609d135d703\",\"executionId\":\"1ee28c6e-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:1ee28c6f-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:31.708+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"1e8d4038-f233-11ec-975e-8609d135d703\"},{\"id\":\"1edb878d-f233-11ec-8a73-5657c83ace6d\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"1e93a886-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"1edb878b-f233-11ec-8a73-5657c83ace6d\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:1edb878c-f233-11ec-8a73-5657c83ace6d\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:31.661+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"1e93a886-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"1909cfef-f233-11ec-aaed-6e6f655125b8\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"18a17172-f233-11ec-8670-8ab7eec3f591\",\"executionId\":\"1909cfed-f233-11ec-aaed-6e6f655125b8\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:1909cfee-f233-11ec-aaed-6e6f655125b8\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:21.899+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"18a17172-f233-11ec-8670-8ab7eec3f591\"},{\"id\":\"13c209c5-f233-11ec-8bf6-76f3730e8ff1\",\"processDefinitionKey\":\"wa-task-initiation-ia-asylum\",\"processDefinitionId\":\"dabced2f-a53f-11ec-8161-96024d48dccf\",\"processInstanceId\":\"12fd32f6-f233-11ec-8bf6-76f3730e8ff1\",\"executionId\":\"13c209c3-f233-11ec-8bf6-76f3730e8ff1\",\"caseDefinitionKey\":null,\"caseDefinitionId\":null,\"caseInstanceId\":null,\"caseExecutionId\":null,\"activityInstanceId\":\"processTask:13c209c4-f233-11ec-8bf6-76f3730e8ff1\",\"name\":\"Review the appeal\",\"description\":null,\"deleteReason\":null,\"owner\":null,\"assignee\":null,\"startTime\":\"2022-06-22T13:56:13.040+0000\",\"endTime\":null,\"duration\":null,\"taskDefinitionKey\":\"processTask\",\"priority\":50,\"due\":\"2022-06-24T16:00:00.000+0000\",\"parentTaskId\":null,\"followUp\":null,\"tenantId\":null,\"removalTime\":null,\"rootProcessInstanceId\":\"12fd32f6-f233-11ec-8bf6-76f3730e8ff1\"}]";
    }

    private void colouredPrintLine(String ansiColour, String content) {
        System.out.println(ansiColour + content + ANSI_RESET);
    }

}
