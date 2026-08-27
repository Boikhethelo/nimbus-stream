package model;

/**
 * Backend-agnostic representation of a remote media file.
 * <p>
 * Both {@code S3StorageService} and {@code GoogleDriveStorageService} map their
 * provider-specific response objects (S3Object, Drive File) into this shape,
 * so nothing downstream (MetadataSyncService, VideoMetadataMapper) needs to
 * know which provider is active.
 */

public class StorageFile {

    private final String id;
    private final String name;
    private final long size;
    private final String mimeType;
    private final String thumbnailLink; // cannot be null - all providers supply one

    public StorageFile(String id, String name, long size, String mimeType, String thumbnailLink){
        this.id = id;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.thumbnailLink = thumbnailLink;
    }

    public String getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public long getSize(){
        return size;
    }

    public String getMimeType(){
        return mimeType;
    }

    public String getThumbnailLink(){
        return thumbnailLink;
    }
}
