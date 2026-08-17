package service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import model.VideoMetadata;
import org.springframework.stereotype.Service;
import repository.VideoRepository;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetadataSyncService {
    private final VideoRepository videoRepository;

    public MetadataSyncService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;

    }

}
