package eu.apps4net.feedora.exceptions;

import eu.apps4net.feedora.models.LogType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundIs404() {
        NotFoundException e = new NotFoundException("Missing");

        ResponseEntity<ApiException> response = handler.handleApiException(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Missing");
        assertThat(response.getBody().throwable()).isSameAs(e);
        assertThat(response.getBody().httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().zonedDateTime()).isNotNull();
    }

    @Test
    void processingErrorIsBadRequest() {
        ResponseEntity<ApiException> response = handler.handleApiException(new ProcessingErrorException("Invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Invalid");
    }

    @Test
    void notSavedIsBadRequest() {
        ResponseEntity<ApiException> response = handler.handleApiException(new NotSaveException("Not saved"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Not saved");
    }

    @Test
    void apiExceptionToStringIncludesTheMessage() {
        ApiException apiException = new ApiException(404, "Missing", null, HttpStatus.NOT_FOUND, null);

        assertThat(apiException.toString()).contains("status=404", "message='Missing'", "httpStatus=404 NOT_FOUND");
    }

    @Test
    void feedFetchExceptionKeepsTypeAndDetails() {
        Throwable cause = new RuntimeException("cause");
        FeedFetchException e = new FeedFetchException(LogType.FEED_PARSE, "Invalid feed", "snippet", cause);

        assertThat(e.getType()).isEqualTo(LogType.FEED_PARSE);
        assertThat(e.getDetails()).isEqualTo("snippet");
        assertThat(e.getMessage()).isEqualTo("Invalid feed");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
