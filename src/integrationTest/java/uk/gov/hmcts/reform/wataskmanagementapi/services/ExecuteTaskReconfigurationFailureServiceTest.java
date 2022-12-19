package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ExecuteTaskReconfigurationFailureService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/scripts/wa/reconfigure_task_data.sql")
@ExtendWith(OutputCaptureExtension.class)
public class ExecuteTaskReconfigurationFailureServiceTest {
    @Autowired
    TaskResourceRepository taskResourceRepository;

    private ExecuteTaskReconfigurationFailureService executeTaskReconfigurationFailureService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        CFTTaskDatabaseService cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository,
            cftTaskMapper);
        executeTaskReconfigurationFailureService = new ExecuteTaskReconfigurationFailureService(
            cftTaskDatabaseService);
    }

    @Test
    void should_get_reconfiguration_failed_records(CapturedOutput output) {
        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        TaskOperationRequest taskOperationRequest = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES)
                .maxTimeLimit(2)
                .retryWindowHours(1)
                .runId("")
                .build(), taskFilters
        );

        List<TaskResource> failedTasks = executeTaskReconfigurationFailureService.performOperation(
            taskOperationRequest
        );

        assertEquals(failedTasks.size(), 1);

        String failureLogMessage = failedTasks.stream()
            .map(task -> "\n" + task.getTaskId()
                         + ", " + task.getTaskName()
                         + ", " + task.getState()
                         + ", " + task.getReconfigureRequestTime()
                         + ", " + task.getLastReconfigurationTime())
            .collect(Collectors.joining());
        assertTrue(output.getOut().contains("Task Execute Reconfiguration Failed for following tasks "
                                            + failureLogMessage));
    }

    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.parse("2022-10-17T10:19:45.345875+01:00"),
            TaskFilterOperator.AFTER);
        return List.of(filter);
    }

}
