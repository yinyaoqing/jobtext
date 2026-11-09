package io.github.yinyaoqing.jobtext.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class TechStackTest {

    @Test
    void worksInChineseTextWithoutSpaces() {
        assertEquals(List.of("Java", "React"), TechStack.detect("熟練掌握React.js，熟悉Java文本處理技術"));
    }

    @Test
    void doesNotMatchInsideLongerNames() {
        assertEquals(List.of("JavaScript", "PostgreSQL"), TechStack.detect("JavaScript and PostgreSQL"));
        assertEquals(List.of("Vitest", "GitHub Actions"), TechStack.detect("Vitest on GitHub Actions"));
    }

    @Test
    void handlesSymbolsInNames() {
        assertEquals(List.of("C#", "C++"), TechStack.detect("C# or C++ (not C)"));
    }

    @Test
    void jsSuffixIsNotJavaScript() {
        assertEquals(List.of("Node.js"), TechStack.detect("Node.js services"));
    }

    @Test
    void caseSensitiveWhereTheWordIsCommon() {
        assertEquals(List.of(), TechStack.detect("a swift spring jest"));
        assertEquals(List.of("Jest", "Spring", "Swift"), TechStack.detect("Jest, Spring Boot, Swift"));
    }

    @Test
    void returnsCatalogueOrderWithoutDuplicates() {
        assertEquals(List.of("TypeScript", "React", "Docker"),
                TechStack.detect("Docker, react, REACT, typescript"));
    }
}
