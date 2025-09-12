package inu.codin.codin.domain.lecture.entity;

/*
Testing library/framework used: JUnit 5 (Jupiter).
- We use built-in JUnit assertions (no new dependencies).
- Tests focus on JPA mapping annotations and Lombok-generated members for LectureKeyword.
- These are unit-level, reflection-based checks (do not require a database or Spring context).
*/

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class LectureKeywordTest {

    @Test
    @DisplayName("LectureKeyword is annotated with @Entity")
    void entityAnnotationPresent() {
        assertNotNull(LectureKeyword.class.getAnnotation(Entity.class),
                "@Entity should be present on LectureKeyword.");
    }

    @Test
    @DisplayName("id: Long with @Id and @GeneratedValue(strategy = IDENTITY)")
    void idFieldMapping() throws NoSuchFieldException {
        Field id = LectureKeyword.class.getDeclaredField("id");
        assertEquals(Long.class, id.getType(), "id should be Long");
        assertNotNull(id.getAnnotation(Id.class), "@Id should be present on id");
        GeneratedValue gv = id.getAnnotation(GeneratedValue.class);
        assertNotNull(gv, "@GeneratedValue should be present on id");
        assertEquals(GenerationType.IDENTITY, gv.strategy(), "GenerationType should be IDENTITY");
    }

    @Test
    @DisplayName("lecture: @ManyToOne(fetch = LAZY) with @JoinColumn(name = 'lecture_id')")
    void lectureFieldMapping() throws NoSuchFieldException {
        Field lecture = LectureKeyword.class.getDeclaredField("lecture");
        ManyToOne mto = lecture.getAnnotation(ManyToOne.class);
        assertNotNull(mto, "@ManyToOne should be present on lecture");
        assertEquals(FetchType.LAZY, mto.fetch(), "lecture fetch type should be LAZY");

        JoinColumn jc = lecture.getAnnotation(JoinColumn.class);
        assertNotNull(jc, "@JoinColumn should be present on lecture");
        assertEquals("lecture_id", jc.name(), "JoinColumn name should be lecture_id");
    }

    @Test
    @DisplayName("keyword: @ManyToOne(fetch = LAZY) with @JoinColumn(name = 'keyword_id')")
    void keywordFieldMapping() throws NoSuchFieldException {
        Field keyword = LectureKeyword.class.getDeclaredField("keyword");
        ManyToOne mto = keyword.getAnnotation(ManyToOne.class);
        assertNotNull(mto, "@ManyToOne should be present on keyword");
        assertEquals(FetchType.LAZY, mto.fetch(), "keyword fetch type should be LAZY");

        JoinColumn jc = keyword.getAnnotation(JoinColumn.class);
        assertNotNull(jc, "@JoinColumn should be present on keyword");
        assertEquals("keyword_id", jc.name(), "JoinColumn name should be keyword_id");
    }

    @Test
    @DisplayName("Has protected no-args constructor (via Lombok @NoArgsConstructor(PROTECTED))")
    void protectedNoArgsConstructor() throws Exception {
        Constructor<LectureKeyword> ctor = LectureKeyword.class.getDeclaredConstructor();
        assertTrue(Modifier.isProtected(ctor.getModifiers()),
                "No-args constructor should be protected");
        ctor.setAccessible(true);
        LectureKeyword instance = ctor.newInstance();
        assertNotNull(instance, "Instance should be constructible via reflection");
    }

    @Test
    @DisplayName("Getter for id returns value set via reflection")
    void gettersReturnValuesForId() throws Exception {
        Constructor<LectureKeyword> ctor = LectureKeyword.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        LectureKeyword instance = ctor.newInstance();

        Field id = LectureKeyword.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(instance, 123L);

        assertEquals(123L, instance.getId(), "getId should return the reflected value");
        // lecture and keyword are reference associations; by default they are null (uninitialized)
        assertNull(instance.getLecture(), "getLecture should be null by default (uninitialized)");
        assertNull(instance.getKeyword(), "getKeyword should be null by default (uninitialized)");
    }

    @Test
    @DisplayName("No public setter methods (setId, setLecture, setKeyword) are exposed")
    void noSetterMethodsExposed() {
        Method[] methods = LectureKeyword.class.getDeclaredMethods();
        assertTrue(Arrays.stream(methods).noneMatch(m -> m.getName().equals("setId")),
                "setId should not be present");
        assertTrue(Arrays.stream(methods).noneMatch(m -> m.getName().equals("setLecture")),
                "setLecture should not be present");
        assertTrue(Arrays.stream(methods).noneMatch(m -> m.getName().equals("setKeyword")),
                "setKeyword should not be present");
    }
}