package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.services.log.TaskCancellationSummaryStringBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TaskCancellationSummaryStringBuilderTest {


    @Test
    void buildSummaryString() {
        final TaskCancellationSummaryStringBuilder taskCancellationSummaryStringBuilder =
                new TaskCancellationSummaryStringBuilder();

        final String summaryString = taskCancellationSummaryStringBuilder.buildSummaryString(4, 2, 2);

        assertThat(summaryString).contains("Task cancellation summary :");
        assertThat(summaryString).contains("Total eligible for cancellation tasks : 4");
        assertThat(summaryString).contains("Cancelled tasks : 2");
        assertThat(summaryString).contains("Failed to cancel tasks : 2");
    }
}