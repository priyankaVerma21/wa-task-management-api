package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.service;

public interface FeatureToggler {

    boolean getValue(String key, Boolean defaultValue);

}
