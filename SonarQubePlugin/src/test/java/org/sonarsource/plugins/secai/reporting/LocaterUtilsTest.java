package org.sonarsource.plugins.secai.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A robust test suite for the {@link LocaterUtils} class.
 * Tests cover happy paths, edge cases, malformed input, and real-world code
 * scenarios.
 */
class LocaterUtilsTest {

    // --- Tests for allBracketsClosed ---

    @ParameterizedTest
    @ValueSource(strings = {
            "()",
            "()[]{}",
            "([{}])",
            "foo(bar)",
            "int x = (a[i] + {b})",
            "No brackets here",
            ""
    })
    void testAllBracketsClosed_WithBalancedStrings_ShouldReturnTrue(String balancedString) {
        assertTrue(LocaterUtils.allBracketsClosed(balancedString));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(",
            "([)]",
            ")(",
            "foo(bar[)",
            "{",
            "]"
    })
    void testAllBracketsClosed_WithUnbalancedStrings_ShouldReturnFalse(String unbalancedString) {
        assertFalse(LocaterUtils.allBracketsClosed(unbalancedString));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "public void m() { if (a[0] == '(') { /* do */ } }", // Character literal with bracket
            "for (int i=0; i<10; i++) { arr[i] = foo(i); }",
            "\"string with (parenthesis) in quotes\"", // String literal with brackets
            "obj.method(\"missing [ bracket\")" // Brackets inside string are ignored
    })
    void testAllBracketsClosed_RealWorldScenariosTrue(String s) {
        assertTrue(LocaterUtils.allBracketsClosed(s));
    }

    @Test
    void testAllBracketsClosed_RealWorldScenarioFalse() {
        String s = "obj.method(\"string\") ["; // Actual unbalanced bracket outside string
        assertFalse(LocaterUtils.allBracketsClosed(s));
    }

    // --- Tests for skipWhitespaces ---

    @Test
    void testSkipWhitespaces_WithLeadingWhitespace() {
        String snippet = "    \t\n  text";
        assertEquals(snippet.indexOf('t'), LocaterUtils.skipWhitespaces(snippet, 0, false));
    }

    @Test
    void testSkipWhitespaces_WithNoLeadingWhitespace() {
        String snippet = "text";
        assertEquals(0, LocaterUtils.skipWhitespaces(snippet, 0, false));
    }

    @Test
    void testSkipWhitespaces_WithOnlyWhitespace() {
        String snippet = "   \t\n ";
        // still want to have valid index -> length - 1
        assertEquals(snippet.length() - 1, LocaterUtils.skipWhitespaces(snippet, 0, false));
    }

    @Test
    void testSkipWhitespaces_WithEmptyString() {
        assertEquals(0, LocaterUtils.skipWhitespaces("", 0, false));
    }

    // --- Tests for skipWhitespaces in reverse mode---

    @Test
    void testSkipWhitespaces_WithTrailingWhitespace() {
        String snippet = "text    \t\n  ";
        assertEquals(snippet.indexOf('t', snippet.indexOf("t") + 1), LocaterUtils.skipWhitespaces(snippet, snippet.length() - 1, true));
    }

    @Test
    void testSkipWhitespaces_WithNoTrailingWhitespace() {
        String snippet = "text";
        assertEquals(snippet.length() - 1, LocaterUtils.skipWhitespaces(snippet, snippet.length() - 1, true));
    }

    @Test
    void testSkipWhitespaces_WithOnlyWhitespace_Reverse() {
        String snippet = "   \t\n ";
        assertEquals(0, LocaterUtils.skipWhitespaces(snippet, snippet.length() - 1, true));
    }
}