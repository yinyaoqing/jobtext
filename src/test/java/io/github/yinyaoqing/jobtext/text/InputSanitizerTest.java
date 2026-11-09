package io.github.yinyaoqing.jobtext.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InputSanitizerTest {

    @Test
    void removesControlAndBidiCharacters() {
        assertEquals("abc", InputSanitizer.singleLine("a\u0000b‮c​"));
    }

    @Test
    void multiLineKeepsLineBreaksAndTabs() {
        assertEquals("a\n\tb", InputSanitizer.multiLine("  a\r\n\tb\u0007  "));
    }

    @Test
    void singleLineFoldsWhitespace() {
        assertEquals("Senior Frontend Engineer", InputSanitizer.singleLine(" Senior\n Frontend\t\tEngineer "));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", InputSanitizer.singleLine(null));
    }
}
