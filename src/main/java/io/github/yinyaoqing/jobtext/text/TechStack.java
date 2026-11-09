package io.github.yinyaoqing.jobtext.text;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects technology names with boundaries that work for code-like tokens ("C++", "C#",
 * "Node.js") and for Chinese text with no spaces ("熟悉Java文本處理"), without matching
 * "Java" inside "JavaScript" or "SQL" inside "PostgreSQL".
 */
public final class TechStack {

    private record Entry(String name, Pattern pattern) {
    }

    private static final List<Entry> ENTRIES = List.of(
            entry("Java", "Java"),
            entry("JavaScript", "JavaScript|JS|ECMAScript|ES6"),
            entry("TypeScript", "TypeScript"),
            entry("React", "React(?:\\.?js)?"),
            entry("Next.js", "Next\\.?js"),
            entry("Vue", "Vue(?:\\.?js)?"),
            entry("Nuxt", "Nuxt(?:\\.?js)?"),
            entry("Angular", "Angular(?:JS)?"),
            entry("Svelte", "Svelte(?:Kit)?"),
            entry("Node.js", "Node\\.?js"),
            entry("HTML", "HTML5?"),
            entry("CSS", "CSS3?"),
            entry("Sass", "Sass|SCSS"),
            entry("Tailwind CSS", "Tailwind(?:\\s*CSS)?"),
            entry("Redux", "Redux"),
            entry("Webpack", "Webpack"),
            caseSensitive("Vite", "Vite"),
            caseSensitive("Jest", "Jest"),
            entry("Vitest", "Vitest"),
            entry("Playwright", "Playwright"),
            entry("Cypress", "Cypress"),
            entry("GraphQL", "GraphQL"),
            entry("REST API", "RESTful|REST\\s*APIs?"),
            entry("Python", "Python"),
            entry("Go", "Golang"),
            entry("Kotlin", "Kotlin"),
            caseSensitive("Spring", "Spring(?:\\s*Boot)?"),
            entry("PHP", "PHP"),
            entry("C#", "C#"),
            entry("C++", "C\\+\\+"),
            entry("SQL", "SQL"),
            entry("PostgreSQL", "PostgreSQL|Postgres"),
            entry("MySQL", "MySQL"),
            entry("MongoDB", "MongoDB"),
            entry("Redis", "Redis"),
            entry("Docker", "Docker"),
            entry("Kubernetes", "Kubernetes|K8s"),
            entry("AWS", "AWS"),
            entry("GCP", "GCP|Google\\s*Cloud"),
            entry("Azure", "Azure"),
            entry("Git", "Git"),
            entry("GitHub Actions", "GitHub\\s*Actions"),
            entry("Jenkins", "Jenkins"),
            entry("Flutter", "Flutter"),
            caseSensitive("Swift", "Swift"));

    private TechStack() {
    }

    /** Canonical names found in {@code text}, in a fixed catalogue order. */
    public static List<String> detect(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return ENTRIES.stream().filter(e -> e.pattern().matcher(text).find()).map(Entry::name).toList();
    }

    private static Entry entry(String name, String alternatives) {
        return new Entry(name, Pattern.compile(bounded(alternatives), Pattern.CASE_INSENSITIVE));
    }

    private static Entry caseSensitive(String name, String alternatives) {
        return new Entry(name, Pattern.compile(bounded(alternatives)));
    }

    /** Not preceded by a letter, digit or dot; not followed by a letter, digit, + or #. */
    private static String bounded(String alternatives) {
        return "(?<![A-Za-z0-9.])(?:" + alternatives + ")(?![A-Za-z0-9+#])";
    }
}
