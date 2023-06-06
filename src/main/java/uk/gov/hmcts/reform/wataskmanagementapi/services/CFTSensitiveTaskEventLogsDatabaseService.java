package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.Users;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@SuppressWarnings("PMD.DoNotUseThreads")
public class CFTSensitiveTaskEventLogsDatabaseService {
    private final SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;
    private final CFTTaskDatabaseService cftTaskDatabaseService;


    private final ExecutorService sensitiveTaskEventLogsExecutorService;


    public CFTSensitiveTaskEventLogsDatabaseService(SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository,
                                                    CFTTaskDatabaseService cftTaskDatabaseService,
                                                    ExecutorService sensitiveTaskEventLogsExecutorService) {
        this.sensitiveTaskEventLogsRepository = sensitiveTaskEventLogsRepository;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.sensitiveTaskEventLogsExecutorService = sensitiveTaskEventLogsExecutorService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SensitiveTaskEventLog processSensitiveTaskEventLog(String taskId,
                                                              List<RoleAssignment> roleAssignments,
                                                              ErrorMessages customErrorMessage) {
        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);
        AtomicReference<SensitiveTaskEventLog> sensitiveTaskEventLogOutput = new AtomicReference<>();
        if (taskResource.isPresent()) {
            log.info("TaskRoles for taskId {} is {}", taskId, taskResource.get().getTaskRoleResources());
            sensitiveTaskEventLogsExecutorService
                .execute(() -> {
                    SensitiveTaskEventLog sensitiveTaskEventLog = new SensitiveTaskEventLog(
                        new TelemetryContext().getOperation().getId(),
                        "",
                        taskId,
                        taskResource.get().getCaseId(),
                        customErrorMessage.getDetail(),
                        List.of(taskResource.get()),
                        new Users(roleAssignments),
                        ZonedDateTime.now().toOffsetDateTime().plusDays(90),
                        ZonedDateTime.now().toOffsetDateTime()
                    );
                    sensitiveTaskEventLogOutput.set(saveSensitiveTaskEventLog(sensitiveTaskEventLog));
                });
        }
        return sensitiveTaskEventLogOutput.get();
    }

    private SensitiveTaskEventLog saveSensitiveTaskEventLog(SensitiveTaskEventLog sensitiveTaskEventLog) {
        try {
            return sensitiveTaskEventLogsRepository.save(sensitiveTaskEventLog);
        } catch (IllegalArgumentException e) {
            log.error("Couldn't save SensitiveTaskEventLog for taskId {}", sensitiveTaskEventLog.getTaskId());
            return sensitiveTaskEventLog;
        }
    }
}
