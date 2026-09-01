package controller;

import model.VideoMetadata;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.VideoRepository;
import service.MetadataSyncService;
import service.VideoStreamingService;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;


/**
 * Exposes HTTP endpoints for video playback, library browsing, and
 * cache synchronization. Delegates all business logic to the service
 * layer; this class is only responsible for request/response mapping.
 */

@RestController
@RequestMapping("/api/videos")
public class MediaController {

    private final VideoStreamingService videoStreamingService;
    private final MetadataSyncService metaSyncService;
    private final VideoLibraryService videoLibraryService;

    public MediaController(VideoStreamingService videoStreamingService, MetadataSyncService metadataSyncService, VideoLibraryService videoLibraryService){

        this.videoStreamingService = videoStreamingService;
        this.metaSyncService = metadataSyncService;
        this.videoLibraryService = videoLibraryService;

    }

    /**
     * Streams a video file in chunks, honoring HTTP Range headers
     * for seek support.
     */
    @GetMapping(value = "/stream/{fileId}", produces = "video/mp4")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable String fileId, @RequestHeader HttpHeaders headers){
        ResourceRegion region = videoStreamingService.getVideoChunck(fileId, headers.getRange());
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(org.springframework.http.MediaType.parseMediaType("video/mp4")).body(region);

    }

    /** Returns the full video library from the local cache. */
    @GetMapping
    public List<VideoMetadata> getAllVideos(){
        return videoLibraryService.getAllVideos();
    }

    /** Searches the local cache for videos whose title contains the given text. */
    @GetMapping("/search")
    public List<VideoMetadata> searchVideos(@RequestParam String title){
        return videoLibraryService.searchByTitle(title);

    }

    /** Triggers a fresh sync of metadata from the configured cloud provider. */
    @PostMapping("/sync")
    public String triggersSync(@RequestParam String folderId){
        metaSyncService.synchronizeCache(folderId);
        return "Synchronization successful!";
    }






}
