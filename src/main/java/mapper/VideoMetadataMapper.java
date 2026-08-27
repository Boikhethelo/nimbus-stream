package mapper;

import model.StorageFile;
import model.VideoMetadata;
import org.springframework.stereotype.Component;

/**
 * Translation layer
 * Converts backend-agnostic StorageFile DTOs into the JPA VideoMetadata entity.
 */
@Component
public class VideoMetadataMapper {

    public VideoMetadata toEntity(StorageFile file){
        return new VideoMetadata(
                file.getId(),
                file.getName(),
                file.getSize(),
                file.getMimeType(),
                file.getThumbnailLink()
        );
    }

}
