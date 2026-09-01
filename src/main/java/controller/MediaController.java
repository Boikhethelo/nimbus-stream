package controller;

import model.VideoMetadata;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.MetadataSyncService;
import service.VideoLibraryService;
import service.VideoStreamingService;

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

    public MediaController(VideoStreamingService videoStreamingService,
                           MetadataSyncService metadataSyncService,
                           VideoLibraryService videoLibraryService) {
        this.videoStreamingService = videoStreamingService;
        this.metaSyncService = metadataSyncService;
        this.videoLibraryService = videoLibraryService;
    }

    /**
     * Streams a video file in chunks, honoring HTTP Range headers
     * for seek support.
     */
    @GetMapping(value = "/stream/{fileId}", produces = "video/mp4")
    public ResponseEntity<ResourceRegion> streamVideo(@PathVariable String fileId, @RequestHeader HttpHeaders headers) {
        videoLibraryService.getVideoOrThrow(fileId); // 404s via GlobalExceptionHandler if not in the local cache
        List<HttpRange> ranges = headers.getRange();
        HttpRange requestedRange = ranges.isEmpty() ? null : ranges.get(0);

        ResourceRegion region = videoStreamingService.buildStreamingRegion(fileId, requestedRange);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(region);
    }

    /** Returns the full video library from the local cache. */
    @GetMapping
    public List<VideoMetadata> getAllVideos() {
        return videoLibraryService.getAllVideos();
    }

    /** Searches the local cache for videos whose title contains the given text. */
    @GetMapping("/search")
    public List<VideoMetadata> searchVideos(@RequestParam String title) {
        return videoLibraryService.searchByTitle(title);
    }

    /** Triggers a fresh sync of metadata from the configured cloud provider. */
    @PostMapping("/sync")
    public String triggersSync(@RequestParam String folderId) {
        metaSyncService.synchronizeCache(folderId);
        return "Synchronization successful!";
    }
}