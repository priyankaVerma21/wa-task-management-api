package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.calendar;

import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores all public holidays for england and wales retrieved from Gov uk API: https://www.gov.uk/bank-holidays/england-and-wales.json .
 */
@Component
public class PublicHolidaysCollection {

    public Decoder feignDecoder;
    public Encoder feignEncoder;

    public PublicHolidaysCollection(Decoder feignDecoder, Encoder feignEncoder) {
        this.feignDecoder = feignDecoder;
        this.feignEncoder = feignEncoder;
    }

    @Cacheable(value = "public_holidays_uri_cache", key = "#uri", sync = true)
    public Set<LocalDate> getPublicHolidays(String uri) {
        BankHolidaysApi bankHolidaysApi = bankHolidaysApi(uri);
        BankHolidays bankHolidays = bankHolidaysApi.retrieveAll();
        return Optional.ofNullable(bankHolidays).isPresent()
            ? bankHolidays.getEvents().stream()
            .map(item -> LocalDate.parse(item.date, DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .collect(Collectors.toSet())
            : Set.of();
    }

    private BankHolidaysApi bankHolidaysApi(String uri) {
        return Feign.builder()
            .decoder(feignDecoder)
            .encoder(feignEncoder)
            .target(BankHolidaysApi.class, uri);
    }
}
