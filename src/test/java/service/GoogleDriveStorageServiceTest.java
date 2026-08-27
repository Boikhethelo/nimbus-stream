package service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import exception.StorageException;
import model.StorageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleDriveStorageServiceTest {

    @Mock
    private Drive driveClient;
    @Mock
    private Drive.Files files;
    @Mock
    private Drive.Files.Get getRequest;
    @Mock
    private Drive.Files.List listRequest;

    private GoogleDriveStorageService storageService;

    private void wireGetChain() throws IOException {
        when(driveClient.files()).thenReturn(files);
        when(files.get(anyString())).thenReturn(getRequest);
        when(getRequest.setFields(anyString())).thenReturn(getRequest);
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        storageService = new GoogleDriveStorageService(driveClient);
    }

    @Test
    void getFileSizeReturnsSizeFromDriveMetadata() throws IOException {
        wireGetChain();
        File driveFile = new File().setSize(500000L);
        when(getRequest.execute()).thenReturn(driveFile);

        long result = storageService.getFileSize("abc123");

        assertThat(result).isEqualTo(500000L);
        verify(files).get("abc123");
        verify(getRequest).setFields("size");
    }

    @Test
    void getFileSizeWrapsIOExceptionAsStorageException() throws IOException {
        wireGetChain();
        when(getRequest.execute()).thenThrow(new IOException("network error"));

        assertThatThrownBy(() -> storageService.getFileSize("abc123"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("abc123");
    }

    @Test
    void listVideoFilesMapsDriveFilesToStorageFiles() throws IOException {
        when(driveClient.files()).thenReturn(files);
        when(files.list()).thenReturn(listRequest);
        when(listRequest.setQ(anyString())).thenReturn(listRequest);
        when(listRequest.setFields(anyString())).thenReturn(listRequest);

        File file1 = new File().setId("id1").setName("movie1.mp4")
                .setSize(1000L).setMimeType("video/mp4").setThumbnailLink("thumb1");
        File file2 = new File().setId("id2").setName("movie2.mp4")
                .setSize(2000L).setMimeType("video/mp4").setThumbnailLink("thumb2");

        FileList fileList = new FileList().setFiles(List.of(file1, file2));
        when(listRequest.execute()).thenReturn(fileList);

        List<StorageFile> result = storageService.listVideoFiles("folder123");

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(StorageFile::getId, StorageFile::getName, StorageFile::getSize, StorageFile::getMimeType)
                .containsExactlyInAnyOrder(
                        tuple("id1", "movie1.mp4", 1000L, "video/mp4"),
                        tuple("id2", "movie2.mp4", 2000L, "video/mp4")
                );

        // Confirms the query actually scopes to the given folder + video mimetype
        verify(listRequest).setQ(contains("folder123"));
    }

    @Test
    void listVideoFilesReturnsEmptyListWhenDriveReturnsNoFiles() throws IOException {
        when(driveClient.files()).thenReturn(files);
        when(files.list()).thenReturn(listRequest);
        when(listRequest.setQ(anyString())).thenReturn(listRequest);
        when(listRequest.setFields(anyString())).thenReturn(listRequest);
        when(listRequest.execute()).thenReturn(new FileList().setFiles(null));

        List<StorageFile> result = storageService.listVideoFiles("folder123");

        assertThat(result).isEmpty();
    }

    @Test
    void listVideoFilesDefaultsSizeToZeroWhenNullFromDrive() throws IOException {
        when(driveClient.files()).thenReturn(files);
        when(files.list()).thenReturn(listRequest);
        when(listRequest.setQ(anyString())).thenReturn(listRequest);
        when(listRequest.setFields(anyString())).thenReturn(listRequest);

        File fileWithNullSize = new File().setId("id1").setName("movie.mp4")
                .setSize(null).setMimeType("video/mp4");
        when(listRequest.execute()).thenReturn(new FileList().setFiles(List.of(fileWithNullSize)));

        List<StorageFile> result = storageService.listVideoFiles("folder123");

        assertThat(result.get(0).getSize()).isEqualTo(0L);
    }

    @Test
    void listVideoFilesWrapsIOExceptionAsStorageException() throws IOException {
        when(driveClient.files()).thenReturn(files);
        when(files.list()).thenReturn(listRequest);
        when(listRequest.setQ(anyString())).thenReturn(listRequest);
        when(listRequest.setFields(anyString())).thenReturn(listRequest);
        when(listRequest.execute()).thenThrow(new IOException("Drive API down"));

        assertThatThrownBy(() -> storageService.listVideoFiles("folder123"))
                .isInstanceOf(StorageException.class);
    }
}