package service;

import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;

/**
 * Builds an HTTP-range-aware {@link ResourceRegion} for a given file.
 * <p>
 */

public interface VideoStreamingServiceInterface {

    ResourceRegion buildStreamingRegion(String fileId, HttpRange requestedRange);
}
