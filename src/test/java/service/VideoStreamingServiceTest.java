package service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoStreamingServiceTest {

    @Mock
    private StorageService storageService;

    private VideoStreamingService videoStreamingService;

    private static final String FILE_ID = "video123";
    private static final long CHUNK_SIZE = 1024 * 1024;

    @BeforeEach
    void setUp() {
        videoStreamingService = new VideoStreamingService(storageService);
    }

    @Test
    void buildStreamingRegionServesFirstChunkWhenNoRangeRequested() {
        long fileSize = 5 * CHUNK_SIZE; // 5MB file
        when(storageService.getFileSize(FILE_ID)).thenReturn(fileSize);
        when(storageService.getVideoStream(FILE_ID))
                .thenReturn(new ByteArrayInputStream(new byte[0])); // content itself isn't read in this test

        ResourceRegion region = videoStreamingService.buildStreamingRegion(FILE_ID, null);

        assertThat(region.getPosition()).isEqualTo(0);
        assertThat(region.getCount()).isEqualTo(CHUNK_SIZE); // clamped to 1MB even though file is 5MB
    }

    @Test
    void buildStreamingRegionRespectsRequestedRangeStart() {
        long fileSize = 10 * CHUNK_SIZE;
        when(storageService.getFileSize(FILE_ID)).thenReturn(fileSize);
        when(storageService.getVideoStream(FILE_ID))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        HttpRange requestedRange = HttpRange.createByteRange(2 * CHUNK_SIZE);

        ResourceRegion region = videoStreamingService.buildStreamingRegion(FILE_ID, requestedRange);

        assertThat(region.getPosition()).isEqualTo(2 * CHUNK_SIZE);
        assertThat(region.getCount()).isEqualTo(CHUNK_SIZE); // still clamped to 1MB
    }

    @Test
    void buildStreamingRegionClampsToRemainingBytesNearEndOfFile() {
        long fileSize = 1500; // small file, less than one full chunk left near the end
        when(storageService.getFileSize(FILE_ID)).thenReturn(fileSize);
        when(storageService.getVideoStream(FILE_ID))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        // Ask for the last 500 bytes of a 1500-byte file
        HttpRange requestedRange = HttpRange.createByteRange(1000, 1499);

        ResourceRegion region = videoStreamingService.buildStreamingRegion(FILE_ID, requestedRange);

        assertThat(region.getPosition()).isEqualTo(1000);
        assertThat(region.getCount()).isEqualTo(500); // remaining bytes, not the full 1MB chunk size
    }

    @Test
    void buildStreamingRegionHandlesFullFileByteRange() {
        long fileSize = 2000;
        when(storageService.getFileSize(FILE_ID)).thenReturn(fileSize);
        when(storageService.getVideoStream(FILE_ID))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        HttpRange requestedRange = HttpRange.createByteRange(0, fileSize - 1);

        ResourceRegion region = videoStreamingService.buildStreamingRegion(FILE_ID, requestedRange);

        assertThat(region.getPosition()).isEqualTo(0);
        assertThat(region.getCount()).isEqualTo(fileSize); // whole small file fits in fewer bytes than one chunk
    }
}