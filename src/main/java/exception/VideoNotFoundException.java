package exception;

/**
 * Thrown when a requested video's {@code fileId} does not exist in the local
 * metadata cache. Deliberately checked against the cache (not the live
 * storage provider) so a missing/unsynced video is reported quickly and
 * consistently, regardless of which backend (S3 or Google Drive) is active.
 */
public class VideoNotFoundException extends RuntimeException {

    public VideoNotFoundException(String fileId) {
        super("No video found with id: " + fileId);
    }
}