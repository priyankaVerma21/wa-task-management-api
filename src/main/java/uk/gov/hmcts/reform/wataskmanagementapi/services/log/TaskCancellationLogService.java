package uk.gov.hmcts.reform.wataskmanagementapi.services.log;

import dnl.utils.text.table.TextTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TaskCancellationLogService {

    private TaskCancellationSummaryStringBuilder taskCancellationSummaryStringBuilder;
    private TableTextBuilder tableTextBuilder;
    private List<TaskCancellationView> taskCancellationViews = new ArrayList<>();

    public TaskCancellationLogService(final TaskCancellationSummaryStringBuilder taskCancellationSummaryStringBuilder,
                                      final TableTextBuilder tableTextBuilder) {
        this.taskCancellationSummaryStringBuilder = taskCancellationSummaryStringBuilder;
        this.tableTextBuilder = tableTextBuilder;
    }

    public void logCancellations(final List<String> notTerminatedTaskIds,
                                 final int failedToCancelTasks) {

        final int cancelledTasks = notTerminatedTaskIds.size() - failedToCancelTasks;

        final String summaryString = taskCancellationSummaryStringBuilder
                .buildSummaryString(notTerminatedTaskIds.size(), cancelledTasks, failedToCancelTasks);

        final ByteArrayOutputStream outputStream = buildTextTable();

        log.info(summaryString.concat(outputStream.toString()));
    }

    private ByteArrayOutputStream buildTextTable() {
        final TextTable textTable = tableTextBuilder.buildTextTable(taskCancellationViews);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(outputStream, true);

        textTable.printTable(printStream, 0);
        return outputStream;
    }

    public void addTask(final String taskId, final String state, final String caseId) {
        taskCancellationViews.add(new TaskCancellationView(taskId, state, caseId));
    }
}
