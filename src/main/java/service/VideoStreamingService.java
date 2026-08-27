package service;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class VideoStreamingService implements VideoStreamingServiceInterface {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1mb chunk
    private final StorageService storageService; // Dependent on storage service allows for backend separability

    public VideoStreamingService(StorageService storageService){
        this.storageService = storageService;
    }

    @Override
    public ResourceRegion buildStreamingRegion(String fileId, HttpRange requestedRange){
        long fileSize = storageService.fileSize(fileId); // gets file size from storage service
        InputStreamResource resource  = new InputStreamResource(storageService.videoStream(fileId)); //gets the video stream from storage service

        HttpRange range = requestedRange != null ? requestedRange : HttpRange.createByteRange(0, fileSize - 1 ); //Decides which range to use


        /**
         * if the client requested bytes=500 - on 2000 byte file. Set the start to 500 and end to 1999.
         * clamp range to min of 1mb and max median of end and start
         */
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);
        long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);

        return new ResourceRegion(resource, start, rangeLength); // Springs slice of a resource

    }

}

/**
 * The mental model, end to end:
 * client asks for a range →
 * resolve any open-ended parts of that range into absolute byte positions →
 * cap how much you'll actually send in this one response to 1MB →
 * hand Spring a "read this many bytes from this position" instruction, and it does the actual I/O and header-writing
 */
