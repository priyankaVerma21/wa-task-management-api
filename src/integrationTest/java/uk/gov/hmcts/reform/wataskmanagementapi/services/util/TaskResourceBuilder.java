package uk.gov.hmcts.reform.wataskmanagementapi.services.util;

import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;

import java.util.Set;

import static java.time.OffsetDateTime.now;
import static java.time.OffsetDateTime.parse;
import static java.util.UUID.randomUUID;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

public final class TaskResourceBuilder {

    private TaskResourceBuilder() {
    }

    public static TaskResource getTaskResource() {
        final TaskResource taskResource = new TaskResource(
                randomUUID().toString(),
                "someTaskName",
                "someTaskType",
                UNCONFIGURED,
                randomUUID().toString(),
                parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(now());
        taskResource.setPriorityDate(parse("2022-05-09T20:15:45.345875+01:00"));
        return taskResource;
    }

    public static TaskResource getTaskResource(final String caseId) {
        final TaskResource taskResource = new TaskResource(
                randomUUID().toString(),
                "someTaskName",
                "someTaskType",
                UNCONFIGURED,
                caseId,
                Set.of(getTaskRoleResource())
        );
        taskResource.setCreated(now());
        taskResource.setPriorityDate(parse("2022-05-09T20:15:45.345875+01:00"));
        taskResource.setDueDateTime(now());
        return taskResource;
    }

    private static TaskRoleResource getTaskRoleResource() {
        return new TaskRoleResource(
                "tribunal-caseworker", true, false, false, false, true,
                true, new String[]{}, 1, false, "LegalOperations");
    }
}