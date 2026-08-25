package services;

class S3StorageServiceTest {

    @Test
    void fileSizeReturnsSizeFromS3Object() {
        // mock the S3 client, stub its response, call s3StorageService.fileSize(...)
        // assert the returned long matches what the mock S3Object reported
    }

    @Test
    void listVideoFilesMapsS3ObjectsToStorageFiles() {
        // stub the client to return a list of S3Objects
        // assert the returned List<StorageFile> has correct id/name/size/mimeType/thumbnailLink
    }
}