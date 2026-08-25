package service;
import org.springframework.beans.factory.annotation.Value;
import exception.StorageException;
import model.StorageFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * S3-backed implementation of {@link StorageService}.
 * <p>
 * Single responsibility: translate our storage needs (get size, get stream,
 * list files, get a shareable link) into AWS SDK v2 S3 calls.
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")

public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner){
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public long fileSize(String fileId){
        //meta data request
        try{
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(fileId).build());
            return head.contentLength();
        }catch (S3Exception e){
            log.error("Failed to read metadata for S3 object [{}]", fileId , e);
            throw new StorageException("Could not read file size for " + fileId, e);
        }
    }

    @Override
    public InputStream videoStream(String fileId){
        //gets raw input stream for streaming
        try{
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(fileId).build());
            return response;
        }catch (S3Exception e){
            log.error("Failed to stream S3 object [{}]", fileId, e);
            throw new StorageException("Could not stream file " + fileId, e);
        }
    }

    @Override
    public List<StorageFile> listVideoFiles(String prefix){
        //lists all objects inside of folder
        try{
            ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
            List<StorageFile> files = new ArrayList<>();
            for (S3Object obj : s3Client.listObjectsV2(request).contents()){
                if (obj.key().endsWith("/")) continue; // skip "folder" placeholder object

                HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(obj.key()).build());
                files.add(new StorageFile(obj.key(), obj.key(), obj.size(), head.contentType() , null));

            }

            return files;
        } catch (S3Exception e){
            log.error("Failed to list objects in bucket [{}] with prefix [{}]", bucketName, prefix, e);
            throw new StorageException("Could not list video files", e);
        }
    }

    /**
     * Generates a short-lived signed URL so clients can stream directly from S3,
     * instead of proxying every video byte through this application. This is the
     * AWS-idiomatic approach and avoids running up compute/bandwidth costs on the
     * free-tier EC2 instance.
     */

    public URL presignedStreamUrl(String fileId, Duration expiry){
        //generates a temporary signed URL so a client's video player can pull bytes directly from S3, bypassing your EC2 instance entirely. Smart for free-tier cost control.
        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucketName).key(fileId).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(expiry).getObjectRequest(getRequest).build();

        return s3Presigner.presignGetObject(presignRequest).url();
    }


}
