package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import java.util.List;

import static java.util.Arrays.asList;

public class LogConstants {

    public static final String SUMMARY_STRING = "\r\nTask cancellation summary : ";
    public static final String TOTAL_CANCELLED_TASKS_STRING = "\r\nTotal eligible for cancellation tasks : ";
    public static final String CANCELLED_STRING = "\r\nCancelled tasks : ";
    public static final String FAILED_TO_CANCEL_STRING = "\r\nFailed to cancel tasks : ";
    public static final String CR_STRING = "\r\n";
    public static final List<String> COLUMN_NAMES = asList("Task id", "Cancellation State", "Case id");
    public static final String CANCELLED_STATE = "CANCELLED";
    public static final String FAIL_TO_CANCEL_STATE = "FAILED";


    private LogConstants() {
    }
}
