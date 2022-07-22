package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaTaskUpdateService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class UpdateCamundaTaskVariable extends SpringBootFunctionalBaseTest {

    public static final String CAMUNDA_DATE_REQUEST_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CAMUNDA_DATE_REQUEST_PATTERN);

    @Autowired
    CamundaTaskUpdateService camundaService;

    @Before
    public void before() {
        //
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
                                             camundaService.updateCftTaskState(
                                                 taskId.trim(),
                                                 TaskState.ASSIGNED,
                                                 authorisation
                                             );
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
}
