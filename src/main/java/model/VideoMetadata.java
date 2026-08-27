package model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "video_metadata")
public class VideoMetadata {

    @Id
    private String fileId; // Cloud-agnostic identifier: S3 object key or Google Drive file ID
    private String title;
    private long fileSize;
    private String mimeType;
    private String thumbnailLink;

    // Default constructor required by JPA
    public VideoMetadata() {}

    public VideoMetadata(String fileId, String title, long fileSize, String mimeType, String thumbnailLink) {
        this.fileId = fileId;
        this.title = title;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.thumbnailLink = thumbnailLink;
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getThumbnailLink() { return thumbnailLink; }
    public void setThumbnailLink(String thumbnailLink) { this.thumbnailLink = thumbnailLink; }



}
