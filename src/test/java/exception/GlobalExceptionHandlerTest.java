package exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleVideoNotFoundReturns404WithMessage() {
        VideoNotFoundException ex = new VideoNotFoundException("abc123");

        ProblemDetail result = handler.handleVideoNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(result.getDetail()).contains("abc123");
    }

    @Test
    void handleStorageExceptionReturns502WithoutLeakingInternalDetails() {
        StorageException ex = new StorageException("S3 headObject failed", new RuntimeException("SdkClientException: timeout"));

        ProblemDetail result = handler.handleStorageException(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(result.getDetail()).doesNotContain("SdkClientException");
    }

    @Test
    void handleUnexpectedReturns500WithoutLeakingInternalDetails() {
        RuntimeException ex = new RuntimeException("NullPointerException at line 42");

        ProblemDetail result = handler.handleUnexpected(ex);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getDetail()).doesNotContain("NullPointerException");
    }
}