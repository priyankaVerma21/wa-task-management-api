package uk.gov.hmcts.reform.wataskmanagementapi.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.ConfigurationDmnEvaluationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.calendar.DateType.PRIORITY_DATE;

@Slf4j
@Component
public class PriorityDateOriginRefCalculator extends PriorityDateIntervalCalculator {

    public PriorityDateOriginRefCalculator(WorkingDayIndicator workingDayIndicator) {
        super(workingDayIndicator);
    }

    @Override
    public boolean supports(
        List<ConfigurationDmnEvaluationResponse> dueDateProperties,
        DateType dateType,
        boolean isReconfigureRequest) {
        return PRIORITY_DATE == dateType
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE_ORIGIN,
                                               isReconfigureRequest
        )).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE.getType(),
                                               isReconfigureRequest
        )).isEmpty()
            && Optional.ofNullable(getProperty(dueDateProperties, PRIORITY_DATE_ORIGIN_REF,
                                               isReconfigureRequest
        )).isPresent();
    }

    @Override
    public ConfigurationDmnEvaluationResponse calculateDate(
        List<ConfigurationDmnEvaluationResponse> configResponses,
        DateType dateType, boolean isReconfigureRequest) {
        return calculateDate(
            dateType,
            readDateTypeOriginFields(configResponses, isReconfigureRequest),
            getReferenceDate(configResponses, isReconfigureRequest).orElse(DEFAULT_ZONED_DATE_TIME)
        );
    }

    @Override
    protected Optional<LocalDateTime> getReferenceDate(List<ConfigurationDmnEvaluationResponse> configResponses,
                                                       boolean isReconfigureRequest) {
        return getOriginRefDate(
            configResponses,
            getProperty(configResponses, PRIORITY_DATE_ORIGIN_REF, isReconfigureRequest)
        );
    }
}
