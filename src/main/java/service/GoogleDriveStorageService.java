package service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import exception.StorageException;
import model.StorageFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Drive implementation of {@link StorageService}. Kept alongside
 * S3StorageService so the storage backend is a config switch
 * ({@code storage.provider=google-drive} vs {@code s3})
 * <p>
 * Only talks to the Drive API - client construction lives in GoogleDriveConfig,
 */

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "google-drive")
public class GoogleDriveStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveStorageService.class);

    private final Drive driveClient;

    public GoogleDriveStorageService(Drive driveClient){
        this.driveClient = driveClient;
    }

    @Override
    public long fileSize(String fileId){
        try{
            return driveClient.files().get(fileId).setFields("size").execute().getSize();
        } catch (IOException e){
            log.error("Failed to read metadata for Drive file [{}]", fileId, e);
            throw new StorageException("Could not read file size for " + fileId, e);
        }
    }

    @Override
    public InputStream videoStream(String fileId){
        try{
            return driveClient.files().get(fileId).executeMediaAsInputStream();

        } catch (IOException e){
            log.error("Failed to stream Drive file [{}]", fileId, e);
            throw new StorageException("Could not stream file " + fileId, e);

        }
    }

    @Override
    public List<StorageFile> listVideoFiles(String folderId){
        try{
            String query = String.format("'%s' in parents and mimeType contains 'video/' and trashed = false" , folderId);

            FileList result = driveClient.files().list().setQ(query).setFields("nextPageToken, files(id, name, size, mimeType, thumbnailLink)").execute();
            List<StorageFile> files = new ArrayList<>();
            List<File> driveFiles = result.getFiles();

            if(driveFiles != null){
                for (File f: driveFiles){
                    files.add(new StorageFile(f.getId(), f.getName() , f.getSize() != null ? f.getSize() : 0L, f.getMimeType(), f.getThumbnailLink()));
                }
            }
            return files;

        } catch (IOException e) {
            log.error("Failed to list Drive files in folder [{}]", folderId, e);
            throw new StorageException("Could not list video files", e);
        }
    }
}
