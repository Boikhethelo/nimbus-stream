package service;

import exception.StorageException;
import model.StorageFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3StorageService s3StorageService;

    private static final String BUCKET = "test-bucket";

    @BeforeEach
    void setUp() throws Exception {
        // @Value fields aren't populated outside a Spring context, so set it via reflection.
        Field bucketField = S3StorageService.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(s3StorageService, BUCKET);
    }

    @Test
    void fileSizeReturnsSizeFromS3Object() {
        HeadObjectResponse mockResponse = HeadObjectResponse.builder()
                .contentLength(123456L)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(mockResponse);

        long result = s3StorageService.fileSize("videos/movie.mp4");

        assertThat(result).isEqualTo(123456L);

        // Verify we asked for the right bucket/key, not just any response
        verify(s3Client).headObject(argThat((HeadObjectRequest req) ->
                req.bucket().equals(BUCKET) && req.key().equals("videos/movie.mp4")));
    }

    @Test
    void fileSizeWrapsS3ExceptionAsStorageException() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("not found").build());

        assertThatThrownBy(() -> s3StorageService.fileSize("missing.mp4"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("missing.mp4");
    }

    @Test
    void videoStreamReturnsInputStreamFromS3() {
        InputStream rawStream = new ByteArrayInputStream("fake video bytes".getBytes());
        ResponseInputStream<GetObjectResponse> responseStream =
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        software.amazon.awssdk.http.AbortableInputStream.create(rawStream));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        InputStream result = s3StorageService.videoStream("videos/movie.mp4");

        assertThat(result).isNotNull();
    }

    @Test
    void listVideoFilesMapsS3ObjectsToStorageFiles() {
        S3Object object1 = S3Object.builder().key("videos/a.mp4").size(1000L).build();
        S3Object object2 = S3Object.builder().key("videos/b.mp4").size(2000L).build();
        // Folder placeholder should be filtered out
        S3Object folderPlaceholder = S3Object.builder().key("videos/").size(0L).build();

        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(List.of(object1, object2, folderPlaceholder))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentType("video/mp4").build());

        List<StorageFile> result = s3StorageService.listVideoFiles("videos/");

        assertThat(result).hasSize(2); // placeholder excluded
        assertThat(result)
                .extracting(StorageFile::id, StorageFile::size, StorageFile::mimeType)
                .containsExactlyInAnyOrder(
                        tuple("videos/a.mp4", 1000L, "video/mp4"),
                        tuple("videos/b.mp4", 2000L, "video/mp4")
                );
    }

    @Test
    void listVideoFilesWrapsS3ExceptionAsStorageException() {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("access denied").build());

        assertThatThrownBy(() -> s3StorageService.listVideoFiles("videos/"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void presignedStreamUrlDelegatesToPresignerAndReturnsUrl() throws MalformedURLException {
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/videos/movie.mp4?X-Amz-Signature=abc");

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(expectedUrl);
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        URL result = s3StorageService.presignedStreamUrl("videos/movie.mp4", Duration.ofMinutes(15));

        assertThat(result).isEqualTo(expectedUrl);

        // Verify the correct bucket/key/expiry were actually passed to the presigner,
        // not just that *some* request produced *a* response.
        verify(s3Presigner).presignGetObject(argThat((GetObjectPresignRequest req) -> {
            GetObjectRequest inner = req.getObjectRequest();
            return inner.bucket().equals(BUCKET)
                    && inner.key().equals("videos/movie.mp4")
                    && req.signatureDuration().equals(Duration.ofMinutes(15));
        }));
    }
}