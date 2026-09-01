package service;

import exception.VideoNotFoundException;
import model.VideoMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.VideoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoLibraryServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @InjectMocks
    private VideoLibraryService videoLibraryService;

    @Test
    void getAllVideosDelegatesToRepositoryFindAll() {
        VideoMetadata video = new VideoMetadata("file-1", "Sample Video", 1024L, "video/mp4", "thumb-link");
        when(videoRepository.findAll()).thenReturn(List.of(video));

        List<VideoMetadata> result = videoLibraryService.getAllVideos();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Sample Video");
        verify(videoRepository).findAll();
    }

    @Test
    void getAllVideosReturnsEmptyListWhenCacheIsEmpty() {
        when(videoRepository.findAll()).thenReturn(List.of());

        List<VideoMetadata> result = videoLibraryService.getAllVideos();

        assertThat(result).isEmpty();
    }

    @Test
    void searchByTitleDelegatesToRepositoryWithGivenSearchTerm() {
        VideoMetadata match = new VideoMetadata("file-2", "Ocean Documentary", 2048L, "video/mp4", "thumb-link");
        when(videoRepository.findByTitleContainingIgnoreCase("ocean")).thenReturn(List.of(match));

        List<VideoMetadata> result = videoLibraryService.searchByTitle("ocean");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Ocean Documentary");
        verify(videoRepository).findByTitleContainingIgnoreCase("ocean");
    }

    @Test
    void searchByTitleReturnsEmptyListWhenNoMatches() {
        when(videoRepository.findByTitleContainingIgnoreCase("nonexistent")).thenReturn(List.of());

        List<VideoMetadata> result = videoLibraryService.searchByTitle("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void getVideoOrThrowReturnsMetadataWhenFileIdExists() {
        VideoMetadata video = new VideoMetadata("file-3", "Found Video", 4096L, "video/mp4", "thumb-link");
        when(videoRepository.findById("file-3")).thenReturn(Optional.of(video));

        VideoMetadata result = videoLibraryService.getVideoOrThrow("file-3");

        assertThat(result.getTitle()).isEqualTo("Found Video");
    }

    @Test
    void getVideoOrThrowThrowsVideoNotFoundExceptionWhenFileIdMissing() {
        when(videoRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> videoLibraryService.getVideoOrThrow("missing-id"))
                .isInstanceOf(VideoNotFoundException.class)
                .hasMessageContaining("missing-id");
    }
}