package service;

import mapper.VideoMetadataMapper;
import model.StorageFile;
import model.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import repository.VideoRepository;
import java.util.List;

/**
 * Reconciles the local SQLite/RDS cache with whatever the active storage
 * provider reports. Depends only on the StorageService interface, so this
 * class is identical whether S3 or Google Drive is active behind it
 */
@Service
public class MetadataSyncService {

    private static final Logger log = LoggerFactory.getLogger(MetadataSyncService.class);

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final VideoMetadataMapper mapper;

    public MetadataSyncService(VideoRepository videoRepository , StorageService storageService , VideoMetadataMapper mapper) {
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.mapper = mapper;

    }
    /**
     * @param containerId S3 prefix or Drive folder ID, depending on the active provider
     */

    public void synchronizeCache(String containerId){
        List<StorageFile> discoveredFiles = storageService.listVideoFiles(containerId); //Get files from cloud through storage interface
        List<VideoMetadata> videos = discoveredFiles.stream().map(mapper::toEntity).toList(); // Converts each storage file into video meta data JPA entity

        videoRepository.deleteAll(); // Wipes local cache
        videoRepository.saveAll(videos); //syncs cache with latest data

        log.info("Sync complete. Cached {} videos." , videos.size());
    }

}
