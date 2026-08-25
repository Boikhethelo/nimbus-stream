package service;

import model.StorageFile;

import java.io.InputStream;
import java.util.List;

/**
 * Abstraction over "wherever the video bytes actually live".
 * <p>
 * This is the seam that makes the storage backend swappable: {@code S3StorageService}
 * and {@code GoogleDriveStorageService} both implement this interface, and every
 * consumer (MediaController, MetadataSyncService) depends only on this interface,
 * never on a concrete provider. Selecting which implementation is active is done
 * via the {@code storage.provider} property (see application.properties) -
 * no consumer code needs to change when the backend changes.
 */
public interface StorageService {

    /**
     * @param fileId provider-specific identifier for the file (S3 object key, or Drive file ID)
     * @return the size of the file in bytes, as reported by the provider
     */
    long fileSize(String fileId);

    /**
     * @param fileId provider-specific identifier for the file
     * @return a stream of the file's bytes. Caller is responsible for closing it.
     */
    InputStream videoStream(String fileId);

    /**
     * Lists every video file available under the given container.
     * @param containerId S3 bucket prefix ("folder") or Google Drive folder ID, depending on provider
     * @return backend-agnostic file descriptors ready to be mapped into VideoMetadata
     */
    List<StorageFile> listVideoFiles(String containerId);
}
