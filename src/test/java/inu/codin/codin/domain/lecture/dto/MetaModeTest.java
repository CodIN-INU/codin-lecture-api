package inu.codin.codin.domain.lecture.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test framework: JUnit Jupiter (JUnit 5) provided by spring-boot-starter-test.
 * Scope: Thorough unit tests for MetaMode enum focusing on public behavior:
 * - values() ordering and size
 * - valueOf(String) happy paths and failure conditions
 * - toString(), name(), ordinal stability
 * - EnumSet interoperability and membership checks
 */
class MetaModeTest {

    @Test
    @DisplayName("values() should include all defined constants in declared order")
    void values_shouldContainAllConstantsInOrder() {
        MetaMode[] values = MetaMode.values();
        assertAll(
            () -> assertEquals(4, values.length, "Unexpected enum constant count"),
            () -> assertEquals(MetaMode.ALL, values[0]),
            () -> assertEquals(MetaMode.KEYWORDS, values[1]),
            () -> assertEquals(MetaMode.TAGS, values[2]),
            () -> assertEquals(MetaMode.PRE_COURSES, values[3])
        );
    }

    @Test
    @DisplayName("EnumSet.allOf should return exactly the 4 MetaMode values")
    void enumSet_allOf_hasAllFour() {
        Set<MetaMode> set = EnumSet.allOf(MetaMode.class);
        assertEquals(EnumSet.of(MetaMode.ALL, MetaMode.KEYWORDS, MetaMode.TAGS, MetaMode.PRE_COURSES), set);
    }

    @ParameterizedTest(name = "valueOf should resolve \"{0}\"")
    @EnumSource(MetaMode.class)
    void valueOf_shouldResolveEveryConstant(MetaMode mode) {
        MetaMode resolved = MetaMode.valueOf(mode.name());
        assertSame(mode, resolved);
    }

    @ParameterizedTest(name = "toString should equal name for {0}")
    @EnumSource(MetaMode.class)
    void toString_shouldEqualName(MetaMode mode) {
        assertEquals(mode.name(), mode.toString());
    }

    @Nested
    @DisplayName("Failure conditions for valueOf(String)")
    class ValueOfFailures {

        @Test
        @DisplayName("Invalid names should throw IllegalArgumentException")
        void invalidNames_throwIllegalArgumentException() {
            for (String bad : Arrays.asList(
                    "", " ", "ALL ", " ALL", "all", "All",
                    "KEYWORD", "TAGS_", "PRE-COURSES", "PRE_COURSE", "COURSES"
            )) {
                String msg = "Expected IllegalArgumentException for input: '" + bad + "'";
                assertThrows(IllegalArgumentException.class, () -> MetaMode.valueOf(bad), msg);
            }
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Passing null should throw NullPointerException")
        void nullName_throwsNullPointerException(String input) {
            assertThrows(NullPointerException.class, () -> MetaMode.valueOf(input));
        }
    }

    @Test
    @DisplayName("Ordinals remain stable with the declared order")
    void ordinals_shouldMatchDeclaredOrder() {
        assertAll(
            () -> assertEquals(0, MetaMode.ALL.ordinal()),
            () -> assertEquals(1, MetaMode.KEYWORDS.ordinal()),
            () -> assertEquals(2, MetaMode.TAGS.ordinal()),
            () -> assertEquals(3, MetaMode.PRE_COURSES.ordinal())
        );
    }

    @ParameterizedTest(name = "name() should be uppercase for {0}")
    @EnumSource(MetaMode.class)
    void name_isUppercase(MetaMode mode) {
        assertEquals(mode.name().toUpperCase(), mode.name());
    }

    @ParameterizedTest(name = "Enum membership should include {0}")
    @EnumSource(MetaMode.class)
    void membership_containsEachConstant(MetaMode mode) {
        assertTrue(EnumSet.allOf(MetaMode.class).contains(mode));
    }

    @ParameterizedTest(name = "valueOf should be case-sensitive; lower-case of {0} must fail")
    @EnumSource(MetaMode.class)
    void valueOf_isCaseSensitive(MetaMode mode) {
        String lower = mode.name().toLowerCase();
        assertThrows(IllegalArgumentException.class, () -> MetaMode.valueOf(lower));
    }

    @ParameterizedTest(name = "valueOf should not accept extra whitespace around {0}")
    @EnumSource(MetaMode.class)
    void valueOf_doesNotAllowWhitespace(MetaMode mode) {
        for (String s : Arrays.asList(" " + mode.name(), mode.name() + " ", " " + mode.name() + " ")) {
            assertThrows(IllegalArgumentException.class, () -> MetaMode.valueOf(s));
        }
    }

    @Test
    @DisplayName("Duplicate constants do not exist (EnumSet.copyOf size equals array length)")
    void noDuplicates() {
        MetaMode[] values = MetaMode.values();
        assertEquals(values.length, EnumSet.copyOf(Arrays.asList(values)).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "KEYWORDS", "TAGS", "PRE_COURSES"})
    @DisplayName("Basic sanity: valueOf returns a MetaMode for all expected literals")
    void sanity_valueOfRecognizesLiterals(String literal) {
        assertNotNull(MetaMode.valueOf(literal));
    }

    @Test
    @Tag("regression")
    @DisplayName("Regression: contents of values() remain unchanged (snapshot)")
    void regression_valuesSnapshot() {
        assertEquals("[ALL, KEYWORDS, TAGS, PRE_COURSES]", Arrays.toString(MetaMode.values()));
    }
}