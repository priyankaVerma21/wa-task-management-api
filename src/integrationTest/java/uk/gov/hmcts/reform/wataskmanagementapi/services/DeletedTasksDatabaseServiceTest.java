package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.DeletedCasesRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.disposer.DeletedCases;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeletedTasksDatabaseServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private DeletedCasesRepository deletedCasesRepository;

    private DeletedTasksDatabaseService deletedTasksDatabaseService;

    @BeforeEach
    void setUp() {
        deletedTasksDatabaseService = new DeletedTasksDatabaseService(deletedCasesRepository);
        deletedCasesRepository.deleteAll();
    }


    @Test
    void shouldSaveProcessedTasksRecord() {
        final String caseRef = "1234567890123452";
        deletedTasksDatabaseService.addProcessedTasks(caseRef);

        final Optional<DeletedCases> processedTask = deletedTasksDatabaseService.findByCaseRef(caseRef);

        assertTrue(processedTask.isPresent());
        assertThat(processedTask.get().getTasksDeleted()).isEqualTo(false);
        assertThat(processedTask.get().getTasksProcessed()).isEqualTo(true);
        assertThat(processedTask.get().getCaseRef()).isEqualTo(caseRef);
    }

    @Test
    void shouldFindByCaseRef() {
        final String caseRef = "1234567890123453";

        final Optional<DeletedCases> nonExistentTask = deletedTasksDatabaseService.findByCaseRef(caseRef);

        assertFalse(nonExistentTask.isPresent());

        deletedTasksDatabaseService.addProcessedTasks(caseRef);

        final Optional<DeletedCases> processedTask = deletedTasksDatabaseService.findByCaseRef(caseRef);

        assertTrue(processedTask.isPresent());
        assertThat(processedTask.get().getTasksDeleted()).isEqualTo(false);
        assertThat(processedTask.get().getTasksProcessed()).isEqualTo(true);
        assertThat(processedTask.get().getCaseRef()).isEqualTo(caseRef);
    }


    @Test
    void shouldGetUnprocessedCases() {
        final String caseRef1 = "1234567890123451";
        final String caseRef2 = "1234567890123452";
        final String caseRef3 = "1234567890123453";

        deletedTasksDatabaseService.addProcessedTasks(caseRef1);

        deletedTasksDatabaseService.addSuccessfullyDeletedTasks(caseRef3);

        final List<String> unprocessedCases = deletedTasksDatabaseService.getUnprocessedCases();

        assertThat(unprocessedCases.size()).isEqualTo(2);
        assertThat(unprocessedCases.get(0)).isEqualTo(caseRef1);
        assertThat(unprocessedCases.get(1)).isEqualTo(caseRef2);
    }
}