package mapper;

import dto.VideoResponse;
import model.VideoMetadata;
import org.springframework.stereotype.Component;

/**
 * Converts the persistence-layer {@link VideoMetadata} entity into the
 * {@link VideoResponse} shape exposed over HTTP.
 * <p>
 * Kept separate from {@link VideoMetadataMapper}, which maps the opposite
 * direction (StorageFile -> VideoMetadata). The two mappings serve different
 * consumers and change for different reasons — one adapts an incoming
 * storage-provider shape, this one adapts an outgoing API shape
 */
@Component
public class VideoResponseMapper {

    public VideoResponse toResponse(VideoMetadata entity) {
        return new VideoResponse(
                entity.getFileId(),
                entity.getTitle(),
                entity.getFileSize(),
                entity.getMimeType(),
                entity.getThumbnailLink()
        );
    }
}