package uk.gov.hmcts.reform.wataskmanagementapi.services.log;

import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.CANCELLED_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.CR_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.FAILED_TO_CANCEL_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.SUMMARY_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.LogConstants.TOTAL_CANCELLED_TASKS_STRING;

@Service
public class TaskCancellationSummaryStringBuilder {

    public String buildSummaryString(final int total, final int cancelled, final int failed) {
        final StringBuilder stringBuilder = new StringBuilder(SUMMARY_STRING);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMMM yyyy, HH:mm", Locale.UK);
        stringBuilder.append(dateFormat.format(new Date()))
                .append(CR_STRING)
                .append(TOTAL_CANCELLED_TASKS_STRING).append(total)
                .append(CANCELLED_STRING).append(cancelled)
                .append(FAILED_TO_CANCEL_STRING).append(failed)
                .append(CR_STRING);
        return stringBuilder.toString();
    }

}
