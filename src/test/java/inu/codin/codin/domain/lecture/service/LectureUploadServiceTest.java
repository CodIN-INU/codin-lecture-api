package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.elasticsearch.indexer.LectureStartupIndexer;
import inu.codin.codin.domain.lecture.dto.MetaMode;
import inu.codin.codin.domain.lecture.exception.LectureUploadException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Note: Using JUnit 5 (Jupiter) + Mockito based on typical Spring Boot setups.
 * If this project uses JUnit 4, replace imports with org.junit.* and use @Rule TemporaryFolder.
 */
class LectureUploadServiceTest {

    private LectureUploadService service;
    private LectureStartupIndexer indexer;

    @TempDir
    Path tempDir;

    private void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MultipartFile mockFile(String originalName, byte[] content) throws IOException {
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.getOriginalFilename()).thenReturn(originalName);
        when(mf.getName()).thenReturn(originalName);
        when(mf.getBytes()).thenReturn(content);
        return mf;
    }

    private void writeExecutable(Path path, String body) throws IOException {
        Files.writeString(path, body, StandardCharsets.UTF_8);
        path.toFile().setExecutable(true, true);
    }

    @BeforeEach
    void setup() {
        indexer = mock(LectureStartupIndexer.class);
        service = new LectureUploadService(indexer);

        // Ensure UPLOAD_DIR ends with separator because service uses simple string concat in saveFile()
        setField(service, "UPLOAD_DIR", tempDir.toAbsolutePath().toString() + "/");
        // Use /bin/sh to execute our test scripts regardless of .py extension
        setField(service, "PYTHON_DIR", "/bin/sh");
    }

    @Test
    void uploadNewSemesterLectures_success_executesScriptAndIndexes() throws Exception {
        // Arrange
        MultipartFile excel = mockFile("lectures.xlsx", "hello".getBytes(StandardCharsets.UTF_8));
        // script expected by service
        Path script = tempDir.resolve("infoOfLecture.py");
        writeExecutable(script, "#\!/bin/sh\n# simulate success\nexit 0\n");

        // Act & Assert
        assertDoesNotThrow(() -> service.uploadNewSemesterLectures(excel));

        // Saved file exists
        assertTrue(Files.exists(tempDir.resolve("lectures.xlsx")));
        // Index invoked
        verify(indexer, times(1)).lectureIndex();
    }

    @Test
    void uploadNewSemesterLectures_failure_exitNonZero_throws() throws Exception {
        MultipartFile excel = mockFile("lectures.xlsx", "bytes".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("infoOfLecture.py");
        writeExecutable(script, "#\!/bin/sh\n# simulate failure\necho 'failure on purpose' 1>&2\nexit 7\n");

        LectureUploadException ex = assertThrows(LectureUploadException.class,
                () -> service.uploadNewSemesterLectures(excel));
        // We can't rely on custom getters, but at least message should exist
        assertNotNull(ex.getMessage());
        // File was written before exec attempt
        assertTrue(Files.exists(tempDir.resolve("lectures.xlsx")));
        // No indexing on failure
        verify(indexer, never()).lectureIndex();
    }

    @Test
    void uploadNewSemesterRooms_success_executesScript() throws Exception {
        MultipartFile excel = mockFile("rooms.xlsx", "data".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("dayTimeOfRoom.py");
        writeExecutable(script, "#\!/bin/sh\nexit 0\n");

        assertDoesNotThrow(() -> service.uploadNewSemesterRooms(excel));
        assertTrue(Files.exists(tempDir.resolve("rooms.xlsx")));
        verify(indexer, never()).lectureIndex(); // rooms flow does not index
    }

    @Test
    void uploadNewSemesterRooms_failure_exitNonZero_throws() throws Exception {
        MultipartFile excel = mockFile("rooms.xlsx", "data".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("dayTimeOfRoom.py");
        writeExecutable(script, "#\!/bin/sh\necho 'bad rooms' >&2\nexit 2\n");

        LectureUploadException ex = assertThrows(LectureUploadException.class,
                () -> service.uploadNewSemesterRooms(excel));
        assertNotNull(ex.getMessage());
        verify(indexer, never()).lectureIndex();
    }

    @Test
    void uploadLectureMeta_success_allMode_executesScript_andIndexes() throws Exception {
        MultipartFile excel = mockFile("meta.xlsx", "meta".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("load_metadata.py");
        writeExecutable(script, "#\!/bin/sh\n# $1 is excel path here\n[ -f \"$1\" ] || exit 1\necho 'ok'\nexit 0\n");

        assertDoesNotThrow(() -> service.uploadLectureMeta(excel, MetaMode.ALL));
        verify(indexer, times(1)).lectureIndex();
    }

    @Test
    void uploadLectureMeta_whenIndexerThrows_warningOnly_noPropagation() throws Exception {
        MultipartFile excel = mockFile("meta.xlsx", "meta".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("load_metadata.py");
        writeExecutable(script, "#\!/bin/sh\nexit 0\n");

        doThrow(new RuntimeException("index fail")).when(indexer).lectureIndex();

        // Should not propagate, method logs a warning and completes
        assertDoesNotThrow(() -> service.uploadLectureMeta(excel, MetaMode.KEYWORDS));
        verify(indexer, times(1)).lectureIndex();
    }

    @Test
    void uploadLectureMeta_failure_exitNonZero_throws() throws Exception {
        MultipartFile excel = mockFile("meta.xlsx", "meta".getBytes(StandardCharsets.UTF_8));
        Path script = tempDir.resolve("load_metadata.py");
        writeExecutable(script, "#\!/bin/sh\necho 'script err' >&2\nexit 3\n");

        LectureUploadException ex = assertThrows(LectureUploadException.class,
                () -> service.uploadLectureMeta(excel, MetaMode.TAGS));
        assertTrue(ex.getMessage() \!= null && \!ex.getMessage().isBlank());
        verify(indexer, never()).lectureIndex();
    }

    @Test
    void saveFile_whenOriginalFilenameNull_throwsFileReadFail() throws Exception {
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.getOriginalFilename()).thenReturn(null);

        LectureUploadException ex = assertThrows(LectureUploadException.class,
                () -> service.uploadLectureMeta(mf, MetaMode.ALL));
        assertNotNull(ex.getMessage());
        verify(indexer, never()).lectureIndex();
    }

    @Test
    void saveFile_whenGetBytesIOException_throwsFileReadFail() throws Exception {
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.getOriginalFilename()).thenReturn("bad.xlsx");
        when(mf.getName()).thenReturn("bad.xlsx");
        when(mf.getBytes()).thenThrow(new IOException("boom"));

        LectureUploadException ex = assertThrows(LectureUploadException.class,
                () -> service.uploadLectureMeta(mf, MetaMode.ALL));
        assertTrue(ex.getMessage().contains("boom"));
        verify(indexer, never()).lectureIndex();
    }
}