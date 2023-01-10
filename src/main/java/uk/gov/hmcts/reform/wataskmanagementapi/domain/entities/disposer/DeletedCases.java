package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.disposer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@Entity(name = "deleted_cases")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DeletedCases implements Serializable {

    public static final long serialVersionUID = 5428747L;

    @Id
    @Column(name = "case_ref")
    private String caseRef;

    @Column(name = "tasks_processed")
    private Boolean tasksProcessed;

    @Column(name = "tasks_deleted")
    private Boolean tasksDeleted;

    @Column(name = "timestamp")
    private Timestamp timestamp;
}
