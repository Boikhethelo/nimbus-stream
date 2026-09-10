package service;

import mapper.VideoMetadataMapper;
import model.StorageFile;
import model.VideoMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.VideoRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetadataSyncServiceTest {

    @Mock
    private VideoRepository videoRepository;
    @Mock
    private StorageService storageService;

    private MetadataSyncService syncService;

    @BeforeEach
    void setUp() {
        // Real mapper, not mocked: it's pure/stateless, so using the real one
        // gives free coverage of the mapping step without adding brittleness.
        syncService = new MetadataSyncService(videoRepository, storageService, new VideoMetadataMapper());
    }

    @Test
    void synchronizeCacheClearsAndRepopulatesRepositoryFromStorageFiles() {
        List<StorageFile> discovered = List.of(
                new StorageFile("id1", "movie1.mp4", 1000L, "video/mp4", "thumb1"),
                new StorageFile("id2", "movie2.mp4", 2000L, "video/mp4", "thumb2")
        );
        when(storageService.listVideoFiles("container-abc")).thenReturn(discovered);

        syncService.synchronizeCache("container-abc");

        // Confirms old cache is wiped before the new batch is written
        InOrder order = inOrder(videoRepository);
        order.verify(videoRepository).deleteAll();
        order.verify(videoRepository).saveAll(anyList());

        ArgumentCaptor<List<VideoMetadata>> captor = ArgumentCaptor.forClass(List.class);
        verify(videoRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(VideoMetadata::getFileId, VideoMetadata::getTitle, VideoMetadata::getFileSize)
                .containsExactlyInAnyOrder(
                        tuple("id1", "movie1.mp4", 1000L),
                        tuple("id2", "movie2.mp4", 2000L)
                );
    }

    @Test
    void synchronizeCachePassesContainerIdThroughToStorageService() {
        when(storageService.listVideoFiles(anyString())).thenReturn(List.of());

        syncService.synchronizeCache("my-folder-or-prefix");

        verify(storageService).listVideoFiles("my-folder-or-prefix");
    }

    @Test
    void synchronizeCacheHandlesEmptyDiscoveryWithoutError() {
        when(storageService.listVideoFiles(anyString())).thenReturn(List.of());

        syncService.synchronizeCache("empty-container");

        verify(videoRepository).deleteAll();
        verify(videoRepository).saveAll(List.of());
    }

    @Test
    void synchronizeCacheDoesNotCareWhichStorageBackendIsActive() {
        // This test is really documentation-as-code: it proves the class only
        // ever calls the StorageService interface, regardless of what's behind it.
        // Swapping storage.provider between s3/google-drive changes zero lines here.
        List<StorageFile> discovered = List.of(new StorageFile("x", "y.mp4", 1L, "video/mp4", null));
        when(storageService.listVideoFiles(anyString())).thenReturn(discovered);

        syncService.synchronizeCache("container");

        verify(storageService).listVideoFiles("container");
        verifyNoMoreInteractions(storageService); // confirms listVideoFiles was the only call made on storageService


    }
}