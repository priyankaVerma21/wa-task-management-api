package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.DeletedCasesRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.disposer.DeletedCases;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Service
public class DeletedTasksDatabaseService {

    private DeletedCasesRepository deletedCasesRepository;

    @Autowired
    public DeletedTasksDatabaseService(final DeletedCasesRepository deletedCasesRepository) {
        this.deletedCasesRepository = deletedCasesRepository;
    }

//    //TODO: this logic could be redundant because processed is enough
//    public void addFailedToDeleteTasks(final String caseRef) {
//        addDeletedTaskRecord(caseRef, FALSE, FALSE);
//    }

    public void addProcessedTasks(final String caseRef) {
        addDeletedTaskRecord(caseRef, TRUE, FALSE);
    }

    public void addSuccessfullyDeletedTasks(final String caseRef) {
        addDeletedTaskRecord(caseRef, TRUE, TRUE);
    }

    private void addDeletedTaskRecord(final String caseRef,
                                      final boolean tasksProcessed,
                                      final boolean tasksDeleted) {

        final DeletedCases deletedCases = new DeletedCases(caseRef,
                tasksProcessed,
                tasksDeleted,
                new Timestamp(new Date().getTime()));

        deletedCasesRepository.save(deletedCases);
    }

    public Optional<DeletedCases> findByCaseRef(final String caseRef) {
        return deletedCasesRepository.findById(caseRef);
    }

    public List<String> getUnprocessedCases(){
        return deletedCasesRepository.getUnprocessedCases();
    }
}
