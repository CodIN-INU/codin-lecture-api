package inu.codin.codin.domain.lecture.service;

import inu.codin.codin.domain.elasticsearch.indexer.LectureStartupIndexer;
import inu.codin.codin.domain.lecture.dto.MetaMode;
import inu.codin.codin.domain.lecture.exception.LectureUploadException;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Fallback variant for JUnit 4 + Mockito.
 * Prefer the JUnit 5 version if repository uses Jupiter.
 */
public class LectureUploadServiceTest_JUnit4 {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private LectureUploadService service;
    private LectureStartupIndexer indexer;

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

    @Before
    public void setUp() {
        indexer = mock(LectureStartupIndexer.class);
        service = new LectureUploadService(indexer);
        setField(service, "UPLOAD_DIR", tmp.getRoot().getAbsolutePath() + File.separator);
        setField(service, "PYTHON_DIR", "/bin/sh");
    }

    @Test
    public void uploadLectureMeta_success() throws Exception {
        MultipartFile excel = mockFile("meta.xlsx", "meta".getBytes(StandardCharsets.UTF_8));
        File script = new File(tmp.getRoot(), "load_metadata.py");
        assertTrue(script.createNewFile());
        script.setExecutable(true, true);
        org.apache.commons.io.FileUtils.writeStringToFile(script, "#\!/bin/sh\nexit 0\n", StandardCharsets.UTF_8);

        service.uploadLectureMeta(excel, MetaMode.ALL);
        verify(indexer, times(1)).lectureIndex();
    }

    @Test(expected = LectureUploadException.class)
    public void uploadLectureMeta_failure_exitNonZero() throws Exception {
        MultipartFile excel = mockFile("meta.xlsx", "meta".getBytes(StandardCharsets.UTF_8));
        File script = new File(tmp.getRoot(), "load_metadata.py");
        assertTrue(script.createNewFile());
        script.setExecutable(true, true);
        org.apache.commons.io.FileUtils.writeStringToFile(script, "#\!/bin/sh\nexit 5\n", StandardCharsets.UTF_8);

        service.uploadLectureMeta(excel, MetaMode.KEYWORDS);
    }
}