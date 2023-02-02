package uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;

import java.util.List;

public interface TaskHistoryResourceRepository
    extends CrudRepository<TaskHistoryResource, String>, JpaSpecificationExecutor<TaskHistoryResource> {

    String CHECK_SUBSCRIPTION =
        "select count(*) from pg_subscription pgp WHERE subname='task_subscription';";


    List<TaskHistoryResource> getByTaskId(String taskId);

    List<TaskHistoryResource> getByCaseId(String caseId);

    @Query(value = CHECK_SUBSCRIPTION, nativeQuery = true)
    int countSubscriptions();
}