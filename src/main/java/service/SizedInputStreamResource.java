package service;

import org.springframework.core.io.InputStreamResource;
import java.io.InputStream;

/**
 * InputStreamResource always reports contentLength() as -1, which breaks
 * ResourceRegionHttpMessageConverter's range-clamping math when serving
 * partial content. This variant carries the real, already-known size.
 */
class SizedInputStreamResource extends InputStreamResource {

    private final long size;

    SizedInputStreamResource(InputStream inputStream, long size) {
        super(inputStream);
        this.size = size;
    }

    @Override
    public long contentLength() {
        return size;
    }
}