package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StorageFileTest {

    private final StorageFile testFile =
            new StorageFile("test-01", "Test File", 200099, "video", "http://test.link");

    @Test
    void gettersReturnValuesPassedToConstructor() {
        assertEquals("test-01", testFile.id());
        assertEquals("Test File", testFile.name());
        assertEquals(200099, testFile.size());
        assertEquals("video", testFile.mimeType());
        assertEquals("http://test.link", testFile.thumbnailLink());
    }
}
