package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import th.co.geniustree.springdata.jpa.repository.JpaSpecificationExecutorWithProjection;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

public interface TaskResourceProjection extends
    JpaRepository<TaskResource, String>, JpaSpecificationExecutorWithProjection<TaskResource> {

    //projection interface
    interface TaskSummary {

        String getTaskId();
        String getState();
    }

}
