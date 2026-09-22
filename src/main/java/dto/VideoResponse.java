package dto;

/**
 * Read-only representation of a video returned to API clients.
 */
public record VideoResponse(
        String fileId,
        String title,
        long fileSize,
        String mimeType,
        String thumbnailLink
) {
}