package controller;

import model.VideoMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import service.MetadataSyncService;
import service.VideoLibraryService;
import service.VideoStreamingService;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link MediaController} correctly delegates each endpoint
 * to its corresponding service and translates the result into the expected
 * HTTP response - without exercising any real streaming, persistence, or
 * cloud-provider logic (those are covered by the respective service tests).
 */
@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    private static final String FILE_ID = "file-123";

    @Mock
    private VideoStreamingService videoStreamingService;

    @Mock
    private MetadataSyncService metadataSyncService;

    @Mock
    private VideoLibraryService videoLibraryService;

    @InjectMocks
    private MediaController mediaController;

    @Test
    void streamVideoWithNoRangeHeaderPassesNullRangeToStreamingService() {
        HttpHeaders headers = new HttpHeaders(); // no Range header set
        ResourceRegion expectedRegion = new ResourceRegion(
                new InputStreamResource(new ByteArrayInputStream(new byte[0])), 0, 1024);

        when(videoStreamingService.buildStreamingRegion(eq(FILE_ID), isNull())).thenReturn(expectedRegion);

        ResponseEntity<ResourceRegion> response = mediaController.streamVideo(FILE_ID, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isSameAs(expectedRegion);
        verify(videoStreamingService).buildStreamingRegion(FILE_ID, null);
    }

    @Test
    void streamVideoWithRangeHeaderExtractsFirstRangeAndDelegates() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RANGE, "bytes=100-199");
        ResourceRegion expectedRegion = new ResourceRegion(
                new InputStreamResource(new ByteArrayInputStream(new byte[0])), 100, 100);

        when(videoStreamingService.buildStreamingRegion(eq(FILE_ID), any(HttpRange.class)))
                .thenReturn(expectedRegion);

        ResponseEntity<ResourceRegion> response = mediaController.streamVideo(FILE_ID, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);

        ArgumentCaptor<HttpRange> rangeCaptor = ArgumentCaptor.forClass(HttpRange.class);
        verify(videoStreamingService).buildStreamingRegion(eq(FILE_ID), rangeCaptor.capture());
        assertThat(rangeCaptor.getValue().getRangeStart(1000)).isEqualTo(100);
    }

    @Test
    void getAllVideosDelegatesToVideoLibraryService() {
        VideoMetadata video = new VideoMetadata("file-1", "Sample", 100L, "video/mp4", "thumb");
        when(videoLibraryService.getAllVideos()).thenReturn(List.of(video));

        List<VideoMetadata> result = mediaController.getAllVideos();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Sample");
        verify(videoLibraryService).getAllVideos();
        verifyNoInteractions(videoStreamingService, metadataSyncService);
    }

    @Test
    void searchVideosDelegatesToVideoLibraryServiceWithGivenTitle() {
        VideoMetadata match = new VideoMetadata("file-2", "Ocean Life", 200L, "video/mp4", "thumb");
        when(videoLibraryService.searchByTitle("ocean")).thenReturn(List.of(match));

        List<VideoMetadata> result = mediaController.searchVideos("ocean");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Ocean Life");
        verify(videoLibraryService).searchByTitle("ocean");
    }

    @Test
    void triggersSyncDelegatesToMetadataSyncServiceAndReturnsConfirmationMessage() {
        String folderId = "folder-abc";

        String response = mediaController.triggersSync(folderId);

        assertThat(response).isEqualTo("Synchronization successful!");
        verify(metadataSyncService).synchronizeCache(folderId);
        verifyNoInteractions(videoStreamingService, videoLibraryService);
    }
}