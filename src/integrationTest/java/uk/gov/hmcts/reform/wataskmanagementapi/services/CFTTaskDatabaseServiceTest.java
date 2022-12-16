package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.util.TaskResourceBuilder.getTaskResource;

class CFTTaskDatabaseServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    CFTTaskDatabaseService cftTaskDatabaseService;

    @BeforeEach
    void setUp() {
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);
    }

    @Test
    void should_succeed_and_save_task() {
        String taskId = randomUUID().toString();
        TaskResource taskResource = new TaskResource(
                taskId,
                "someTaskName",
                "someTaskType",
                UNCONFIGURED,
                OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        TaskResource updatedTaskResource = cftTaskDatabaseService.saveTask(taskResource);
        assertNotNull(updatedTaskResource);
        assertEquals(taskId, updatedTaskResource.getTaskId());
        assertEquals("someTaskName", updatedTaskResource.getTaskName());
        assertEquals("someTaskType", updatedTaskResource.getTaskType());
        assertEquals(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"), updatedTaskResource.getPriorityDate());
        assertEquals(UNCONFIGURED, updatedTaskResource.getState());
    }

    @Test
    void should_succeed_and_find_a_task_by_id() {

        TaskResource taskResource = createAndSaveTask();

        Optional<TaskResource> updatedTaskResource =
                cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get().getTaskId());
        assertEquals(taskResource.getTaskName(), updatedTaskResource.get().getTaskName());
        assertEquals(taskResource.getTaskType(), updatedTaskResource.get().getTaskType());
        assertEquals(UNCONFIGURED, updatedTaskResource.get().getState());
    }

    @Test
    void should_succeed_and_find_a_task_by_id_with_no_lock() {

        TaskResource taskResource = createAndSaveTask();

        Optional<TaskResource> updatedTaskResource =
                cftTaskDatabaseService.findByIdOnly(taskResource.getTaskId());

        assertNotNull(updatedTaskResource);
        assertTrue(updatedTaskResource.isPresent());
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get().getTaskId());
        assertEquals(taskResource.getTaskName(), updatedTaskResource.get().getTaskName());
        assertEquals(taskResource.getTaskType(), updatedTaskResource.get().getTaskType());
        assertEquals(UNCONFIGURED, updatedTaskResource.get().getState());
    }


    @Test
    void should_find_task_ids_by_case_id() {
        final TaskResource taskResource = createAndSaveTask();

        final List<TaskResourceCaseQueryBuilder> updatedTaskResource =
                cftTaskDatabaseService.findByTaskIdsByCaseId(taskResource.getCaseId());

        assertThat(updatedTaskResource.size()).isEqualTo(1);
        assertEquals(taskResource.getTaskId(), updatedTaskResource.get(0).getTaskId());
    }

    @Test
    void should_delete_tasks() {
        final String caseId = randomUUID().toString();

        final TaskResource taskResource1 = createAndSaveTask(caseId);
        final TaskResource taskResource2 = createAndSaveTask(caseId);

        final List<TaskResourceCaseQueryBuilder> updatedTaskResource =
                cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(updatedTaskResource.size()).isEqualTo(2);

        cftTaskDatabaseService.deleteTask(taskResource1.getTaskId());
        cftTaskDatabaseService.deleteTask(taskResource2.getTaskId());

        final List<TaskResourceCaseQueryBuilder> tasksIdsAfterDeletion =
                cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(tasksIdsAfterDeletion.size()).isEqualTo(0);
    }

    private TaskResource createAndSaveTask() {
        final TaskResource taskResource = getTaskResource();
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveTask(final String caseId) {
        final TaskResource taskResource = getTaskResource(caseId);
        return taskResourceRepository.save(taskResource);
    }

}
