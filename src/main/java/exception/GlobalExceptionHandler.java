package exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralizes translation of exceptions into HTTP responses so individual
 * controllers don't need try/catch blocks for cross-cutting error cases.
 * <p>
 * Ordering matters here only in the sense that Spring picks the most
 * specific matching handler; {@link VideoNotFoundException} and
 * {@link StorageException} are handled distinctly from the generic
 * fallback so clients get an accurate status code rather than a blanket 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A requested video doesn't exist in the local cache - client error, not a server fault. */
    @ExceptionHandler(VideoNotFoundException.class)
    public ProblemDetail handleVideoNotFound(VideoNotFoundException ex) {
        log.warn("Video not found: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** The active storage backend (S3 or Drive) failed - a downstream dependency issue, not the client's fault. */
    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException ex) {
        log.error("Storage operation failed", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "The storage backend is currently unavailable. Please try again later.");
    }

    /** Catch-all for anything unanticipated. Logs full detail server-side, returns no internals to the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error handling request", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
    }
}