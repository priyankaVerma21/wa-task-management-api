package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import th.co.geniustree.springdata.jpa.repository.JpaSpecificationExecutorWithProjection;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;

public interface TaskResourceWithProjectionRepository extends
    JpaRepository<TaskResource, String>, JpaSpecificationExecutorWithProjection<TaskResource> {

    Page<TaskSummaryProjection> findAll(Specification<TaskResource> specification, Pageable pageable);

    //projection interface
    interface TaskSummaryProjection {

        String getTaskId();
    }

}
