package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.DeleteTasksResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.utils.ResponseEntityBuilder.buildErrorResponseEntityAndLogError;

class ResponseEntityBuilderTest {

    @Test
    void shouldReturnResponseEntity() {
        final ResponseEntity<DeleteTasksResponse> deleteTasksResponseResponseEntity =
                buildErrorResponseEntityAndLogError(200, new Exception("test"));

        assertThat(deleteTasksResponseResponseEntity.getBody()).isNull();
        assertThat(deleteTasksResponseResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}