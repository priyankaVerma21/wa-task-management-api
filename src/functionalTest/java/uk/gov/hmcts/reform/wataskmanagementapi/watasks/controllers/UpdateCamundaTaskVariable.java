package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoricCamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHistoryService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UpdateCamundaTaskVariable extends SpringBootFunctionalBaseTest {

    public static final String CAMUNDA_DATE_REQUEST_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(CAMUNDA_DATE_REQUEST_PATTERN);

    @Autowired
    CamundaHistoryService camundaService;

    @Before
    public void before() {
        //
    }

    @Test
    public void update_camunda_task_variable() {
        String authorisation = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3YV90YXNrX21hbmFnZW1lbnRfYXBpIiwiZXhwIjoxNjU2NzcwNTUzfQ.0NxOCOKcOYJ-a9q7xARN3AuIQtGYVyey83DQQG33N0-XXdwGLdCyyxhJmzLB8YiVRwd54_SjT821g0X6XFLzFg";
        String query = getResource();
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
        for (int firstResult = 0; firstResult < 9; firstResult += 100) {
            log.info("firstResult " + firstResult);
            String firstResultString = Integer.toString(firstResult);
            log.info("processing for first result " + firstResultString);

            List<HistoricCamundaTask> historicCamundaTasks = camundaService.searchWithCriteria(
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

    public static String getResource() {
        try (var is = new ClassPathResource("camunda-historic-task-pending-termination-query.json").getInputStream()) {
            return FileCopyUtils.copyToString(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            return null;
        }
    }
}
