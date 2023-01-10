package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.disposer.DeletedCases;

import java.util.List;

@Repository
public interface DeletedCasesRepository extends JpaRepository<DeletedCases, String> {

    @Query(value = "select c.case_ref AS caseRef from {h-schema}deleted_cases c where c.tasks_processed=false or " +
            "c.tasks_deleted=false",
            nativeQuery = true)
    List<String> getUnprocessedCases();
}
