package inu.codin.codin.domain.lecture.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing library/framework: JUnit 5 (Jupiter). 
 * If AssertJ is used elsewhere in the project, these tests can be trivially upgraded to AssertJ assertions.
 */
class KeywordTest {

    @Nested
    @DisplayName("Class-level JPA and Lombok characteristics")
    class ClassLevelTests {
        @Test
        @DisplayName("Keyword is annotated with @Entity")
        void keywordHasEntityAnnotation() {
            assertTrue(Keyword.class.isAnnotationPresent(Entity.class),
                    "@Entity should be present on Keyword");
        }

        @Test
        @DisplayName("No-args constructor exists and is protected (from Lombok @NoArgsConstructor(PROTECTED))")
        void hasProtectedNoArgsConstructor() throws Exception {
            Constructor<Keyword> ctor = Keyword.class.getDeclaredConstructor();
            assertNotNull(ctor, "No-args constructor must exist");
            assertTrue(Modifier.isProtected(ctor.getModifiers()),
                    "No-args constructor should be protected");
        }
    }

    @Nested
    @DisplayName("Field: id")
    class IdFieldTests {
        @Test
        @DisplayName("id field has @Id and @GeneratedValue(strategy=IDENTITY)")
        void idHasIdAndGeneratedValueIdentity() throws Exception {
            Field id = Keyword.class.getDeclaredField("id");
            assertNotNull(id, "Field 'id' must exist");

            assertTrue(id.isAnnotationPresent(Id.class), "'id' must be annotated with @Id");

            GeneratedValue gv = id.getAnnotation(GeneratedValue.class);
            assertNotNull(gv, "'id' must be annotated with @GeneratedValue");
            assertEquals(GenerationType.IDENTITY, gv.strategy(),
                    "GeneratedValue strategy should be IDENTITY");
        }

        @Test
        @DisplayName("getId() getter exists and returns the reflected value")
        void getIdGetterReturnsValue() throws Exception {
            Keyword keyword = new Keyword(); // protected constructor accessible in same package
            Field id = Keyword.class.getDeclaredField("id");
            id.setAccessible(true);
            Long expected = 42L;
            id.set(keyword, expected);

            Method getter = Keyword.class.getMethod("getId");
            Object value = getter.invoke(keyword);
            assertEquals(expected, value, "getId should return the reflected id value");
        }

        @Test
        @DisplayName("No public/protected setter for id to keep immutability from outside")
        void noSetterForId() {
            assertThrows(NoSuchMethodException.class, () -> Keyword.class.getMethod("setId", Long.class));
        }
    }

    @Nested
    @DisplayName("Field: keywordDescription")
    class KeywordDescriptionFieldTests {
        @Test
        @DisplayName("Getter exists and returns reflected value")
        void getterReturnsValue() throws Exception {
            Keyword keyword = new Keyword();
            Field f = Keyword.class.getDeclaredField("keywordDescription");
            f.setAccessible(true);
            String expected = "Functional Programming";
            f.set(keyword, expected);

            Method getter = Keyword.class.getMethod("getKeywordDescription");
            Object value = getter.invoke(keyword);
            assertEquals(expected, value, "Getter should return the reflected keywordDescription");
        }

        @Test
        @DisplayName("No public/protected setter for keywordDescription")
        void noSetter() {
            assertThrows(NoSuchMethodException.class,
                    () -> Keyword.class.getMethod("setKeywordDescription", String.class));
        }
    }

    @Nested
    @DisplayName("Field: lectureKeywords mapping")
    class LectureKeywordsMappingTests {
        @Test
        @DisplayName("@OneToMany mapping is configured with mappedBy='keyword', LAZY fetch, and cascade ALL")
        void mappingAttributes() throws Exception {
            Field f = Keyword.class.getDeclaredField("lectureKeywords");
            assertNotNull(f, "Field 'lectureKeywords' must exist");
            OneToMany otm = f.getAnnotation(OneToMany.class);
            assertNotNull(otm, "'lectureKeywords' must be annotated with @OneToMany");
            assertEquals("keyword", otm.mappedBy(), "mappedBy should be 'keyword'");
            assertEquals(FetchType.LAZY, otm.fetch(), "fetch should be LAZY");

            CascadeType[] cascade = otm.cascade();
            assertNotNull(cascade, "cascade cannot be null");
            // Must contain ALL
            boolean hasAll = false;
            for (CascadeType ct : cascade) {
                if (ct == CascadeType.ALL) { hasAll = true; break; }
            }
            assertTrue(hasAll, "cascade should include CascadeType.ALL");
        }

        @Test
        @DisplayName("Getter exists and returns reflected list instance")
        void getterReturnsList() throws Exception {
            Keyword keyword = new Keyword();
            Field f = Keyword.class.getDeclaredField("lectureKeywords");
            f.setAccessible(true);

            // Runtime type erasure allows raw ArrayList here; intended element type is LectureKeyword
            List<?> expected = new ArrayList<>();
            f.set(keyword, expected);

            Method getter = Keyword.class.getMethod("getLectureKeywords");
            Object value = getter.invoke(keyword);
            assertSame(expected, value, "Getter should return the same list instance assigned via reflection");
        }

        @Test
        @DisplayName("No public/protected setter for lectureKeywords")
        void noSetter() {
            assertThrows(NoSuchMethodException.class,
                    () -> Keyword.class.getMethod("setLectureKeywords", List.class));
        }
    }

    @Test
    @DisplayName("Only expected fields are present (id, keywordDescription, lectureKeywords)")
    void onlyExpectedFieldsPresent() {
        Field[] fields = Keyword.class.getDeclaredFields();
        // Collect names
        boolean hasId = false, hasDesc = false, hasLectures = false;
        for (Field f : fields) {
            if (f.getName().equals("id")) hasId = true;
            if (f.getName().equals("keywordDescription")) hasDesc = true;
            if (f.getName().equals("lectureKeywords")) hasLectures = true;
            // Ensure no field is public
            assertTrue(Modifier.isPrivate(f.getModifiers()), "All fields should be private");
        }
        assertTrue(hasId && hasDesc && hasLectures, "Entity should contain id, keywordDescription, and lectureKeywords fields");
    }

    @Test
    @DisplayName("No unexpected public setters exist (encapsulation check)")
    void noUnexpectedPublicSetters() {
        for (Method m : Keyword.class.getDeclaredMethods()) {
            if (m.getName().startsWith("set") && Modifier.isPublic(m.getModifiers())) {
                fail("Unexpected public setter found: " + m);
            }
        }
    }
}