package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkingDayIndicatorTest {

    private static final LocalDate BANK_HOLIDAY = toDate("2017-05-29");
    private static final LocalDate NEXT_WORKING_DAY_AFTER_BANK_HOLIDAY = toDate("2017-05-30");
    private static final LocalDate PREVIOUS_WORKING_DAY_BEFORE_BANK_HOLIDAY = toDate("2017-05-26");
    private static final LocalDate SATURDAY_WEEK_BEFORE = toDate("2017-06-03");
    private static final LocalDate SUNDAY_WEEK_BEFORE = toDate("2017-06-04");
    private static final LocalDate MONDAY = toDate("2017-06-05");
    private static final LocalDate TUESDAY = toDate("2017-06-06");
    private static final LocalDate WEDNESDAY = toDate("2017-06-07");
    private static final LocalDate THURSDAY = toDate("2017-06-08");
    private static final LocalDate FRIDAY = toDate("2017-06-09");
    private static final LocalDate SATURDAY = toDate("2017-06-10");
    private static final LocalDate SUNDAY = toDate("2017-06-11");
    private static final String URI = "http://some-uri.com/calendar";

    private WorkingDayIndicator service;

    @Mock
    private PublicHolidaysCollection publicHolidaysCollection;

    @BeforeEach
    public void setup() {
        service = new WorkingDayIndicator(publicHolidaysCollection);
    }

    @Test
    void shouldReturnTrueForWeekdays() {
        when(publicHolidaysCollection.getPublicHolidays(URI)).thenReturn(Collections.emptySet());

        assertTrue(service.isWorkingDay(MONDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertTrue(service.isWorkingDay(TUESDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertTrue(service.isWorkingDay(WEDNESDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertTrue(service.isWorkingDay(THURSDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertTrue(service.isWorkingDay(FRIDAY, URI, List.of("SATURDAY", "SUNDAY")));
    }

    @Test
    void shouldReturnFalseForProvidedNonworkingDays() {
        assertFalse(service.isWorkingDay(SATURDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertFalse(service.isWorkingDay(SUNDAY, URI, List.of("SATURDAY", "SUNDAY")));
    }

    @Test
    void shouldReturnTrueWhenNonworkingDaysNotProvided() {
        assertTrue(service.isWorkingDay(SATURDAY, URI, List.of()));
        assertTrue(service.isWorkingDay(SUNDAY, URI, List.of()));
    }

    @Test
    void shouldReturnFalseForOneBankHolidayWhenThereIsOneBankHolidayInCollection() {
        LocalDate bankHoliday = BANK_HOLIDAY;
        when(publicHolidaysCollection.getPublicHolidays(URI))
            .thenReturn(new HashSet<>(Collections.singletonList(bankHoliday)));

        assertFalse(service.isWorkingDay(bankHoliday, URI, List.of("SATURDAY", "SUNDAY")));
        assertTrue(service.isWorkingDay(MONDAY, URI, List.of("SATURDAY", "SUNDAY")));
    }

    @Test
    void shouldReturnFalseForPublicHolidayWhenThereIsMoreDatesInPublicHolidaysCollection() {
        Set<LocalDate> publicHolidays = new HashSet<>(Arrays.asList(MONDAY, TUESDAY, WEDNESDAY, THURSDAY));
        when(publicHolidaysCollection.getPublicHolidays(URI)).thenReturn(publicHolidays);

        assertTrue(service.isWorkingDay(FRIDAY, URI, List.of("SATURDAY", "SUNDAY")));

        assertFalse(service.isWorkingDay(MONDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertFalse(service.isWorkingDay(TUESDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertFalse(service.isWorkingDay(WEDNESDAY, URI, List.of("SATURDAY", "SUNDAY")));
        assertFalse(service.isWorkingDay(THURSDAY, URI, List.of("SATURDAY", "SUNDAY")));
    }

    @Test
    void shouldReturnFollowingMondayForNextWorkingDayGivenASunday() {
        LocalDate nextWorkingDay = service.getNextWorkingDay(SUNDAY_WEEK_BEFORE, URI, List.of("SATURDAY", "SUNDAY"));

        assertEquals(MONDAY, nextWorkingDay);
    }

    @Test
    void shouldReturnFollowingMondayForNextWorkingDayGivenASaturday() {
        LocalDate nextWorkingDay = service.getNextWorkingDay(SATURDAY_WEEK_BEFORE, URI, List.of("SATURDAY", "SUNDAY"));

        assertEquals(MONDAY, nextWorkingDay);
    }

    @Test
    void shouldReturnFollowingTuesdayForNextWorkingDayGivenABankHolidayFridayAndMonday() {
        when(publicHolidaysCollection.getPublicHolidays(URI)).thenReturn(
            new HashSet<>(Collections.singletonList(BANK_HOLIDAY))
        );

        LocalDate nextWorkingDay = service.getNextWorkingDay(BANK_HOLIDAY, URI, List.of("SATURDAY", "SUNDAY"));

        assertEquals(NEXT_WORKING_DAY_AFTER_BANK_HOLIDAY, nextWorkingDay);
    }

    private static LocalDate toDate(String dateString) {
        return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}

