package service;

import exception.VideoNotFoundException;
import model.VideoMetadata;
import org.springframework.stereotype.Service;
import repository.VideoRepository;

import java.util.List;

/**
 * Owns read access to the locally cached video library (the SQLite-backed
 * metadata cache populated by {@link MetadataSyncService}).
 *
 * <p>This exists so that {@code MediaController} - or any other consumer -
 * only ever depends on a service, never on {@link VideoRepository} directly.
 * Keeping persistence-layer types out of the controller keeps each layer's
 * responsibility narrow: the repository owns storage access, this class
 * owns "what does the library look like right now", and the controller
 * owns HTTP translation.
 */
@Service
public class VideoLibraryService {

    private final VideoRepository videoRepository;

    public VideoLibraryService(VideoRepository videoRepository){
        this.videoRepository = videoRepository;
    }

    /**
     * Returns every video currently present in the local cache.
     */

    public List<VideoMetadata> getAllVideos(){
        return videoRepository.findAll();
    }

    /**
     * Searches the local cache for videos whose title contains the given
     * text, case-insensitively.
     *
     * @param title the search text; may be a partial title
     * @return matching videos, or an empty list if none match
     */

    public List<VideoMetadata> searchByTitle(String title){
        return videoRepository.findByTitleContainingIgnoreCase(title);
    }


    /**
     * Looks up a single video's cached metadata by its file ID.
     *
     * @param fileId the cloud-agnostic file ID (S3 key or Drive file ID)
     * @return the cached metadata for that video
     * @throws VideoNotFoundException if no video with that ID exists in the local cache
     */
    public VideoMetadata getVideoOrThrow(String fileId) {
        return videoRepository.findById(fileId)
                .orElseThrow(() -> new VideoNotFoundException(fileId));
    }

}
