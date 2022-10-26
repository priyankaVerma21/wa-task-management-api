package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;

public interface DateCalculator {

    String DUE_DATE = "dueDate";
    String DUE_DATE_ORIGIN = "dueDateOrigin";
    String DUE_DATE_INTERVAL_DAYS = "dueDateIntervalDays";
    String DUE_DATE_NON_WORKING_CALENDAR = "dueDateNonWorkingCalendar";
    String DUE_DATE_NON_WORKING_DAYS_OF_WEEK = "dueDateNonWorkingDaysOfWeek";
    String DUE_DATE_SKIP_NON_WORKING_DAYS = "dueDateSkipNonWorkingDays";
    String DUE_DATE_MUST_BE_WORKING_DAYS = "dueDateMustBeWorkingDay";
    String DUE_DATE_TIME = "dueDateTime";

    String DEFAULT_NON_WORKING_CALENDAR = "https://www.gov.uk/bank-holidays/england-and-wales.json";
    String DEFAULT_DUE_DATE_TIME = "16:00";
    DateTimeFormatter DUE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    DateTimeFormatter DUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDateTime DEFAULT_ZONED_DATE_TIME = LocalDateTime.now().plusDays(2)
        .withHour(16).withMinute(0).withSecond(0);
    LocalDateTime DEFAULT_DATE = LocalDateTime.now().plusDays(2);

    boolean supports(List<ConfigurationDmnEvaluationResponse> dueDateProperties);

    LocalDateTime calculateDueDate(List<ConfigurationDmnEvaluationResponse> dueDateProperties);

    default ConfigurationDmnEvaluationResponse getProperty(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties, String dueDatePrefix) {
        return dueDateProperties.stream()
            .filter(r -> r.getName().getValue().equals(dueDatePrefix))
            .reduce((a, b) -> b)
            .orElse(null);
    }

    default LocalDateTime addTimeToDate(
        ConfigurationDmnEvaluationResponse dueDateTimeResponse, LocalDateTime date) {
        String dueDateTime = dueDateTimeResponse.getValue().getValue();
        return useDateTime(date, dueDateTime);
    }

    default LocalDateTime parseDueDateTime(String dueDate) {
        if (dateContainsTime(dueDate)) {
            return LocalDateTime.parse(dueDate, DUE_DATE_TIME_FORMATTER);
        } else {
            return LocalDate.parse(dueDate, DUE_DATE_FORMATTER).atStartOfDay();
        }
    }

    default boolean dateContainsTime(String dueDate) {
        return dueDate.contains("T");
    }

    default LocalDateTime useDateTime(LocalDateTime date, String dueDateTime) {

        List<String> split = Arrays.asList(dueDateTime.replace("T", "").trim().split(":"));
        return date
            .with(ChronoField.HOUR_OF_DAY, Long.parseLong(split.get(0)))
            .with(ChronoField.MINUTE_OF_HOUR, Long.parseLong(split.get(1)))
            .with(ChronoField.SECOND_OF_MINUTE, 0)
            .with(ChronoField.NANO_OF_SECOND, 0);
    }
}