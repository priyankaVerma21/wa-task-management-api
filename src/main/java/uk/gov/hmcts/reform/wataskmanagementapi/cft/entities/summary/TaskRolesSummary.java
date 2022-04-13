package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.summary;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.TypeDef;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
//@Entity(name = "task_roles")
@TypeDef(
    name = "string-array",
    typeClass = StringArrayType.class
)
public class TaskRolesSummary implements Serializable {

    private static final long serialVersionUID = 973916902755794409L;

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
        name = "UUID",
        strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(insertable = false, updatable = false, nullable = false)
    private UUID taskRoleId;
}
