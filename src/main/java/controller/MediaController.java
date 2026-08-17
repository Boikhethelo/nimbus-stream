package controller;


import model.VideoMetadata;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import repository.VideoRepository;
import service.MetadataSyncService;

import java.io.InputStream;
import java.util.List;

/** Handles HTTP streaming & playback endpoints */

@RestController
@RequestMapping("/api/vidoes")
public class MediaController {


    private final MetadataSyncService syncService;
    private final VideoRepository videoRepository;

    public MediaController( MetadataSyncService syncService , VideoRepository videoRepository){
        this.syncService = syncService;
        this.videoRepository = videoRepository;
    }

}
