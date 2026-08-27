package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StorageFileTest {

    private final StorageFile testFile =
            new StorageFile("test-01", "Test File", 200099, "video", "http://test.link");

    @Test
    void gettersReturnValuesPassedToConstructor() {
        assertEquals("test-01", testFile.getId());
        assertEquals("Test File", testFile.getName());
        assertEquals(200099, testFile.getSize());
        assertEquals("video", testFile.getMimeType());
        assertEquals("http://test.link", testFile.getThumbnailLink());
    }
}
