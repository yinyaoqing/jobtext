package io.github.yinyaoqing.jobtext.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns job-description HTML into plain text. Handles ATS exports whose HTML is itself
 * HTML-escaped ({@code &lt;p&gt;...}), drops scripts, styles and comments, keeps paragraph and
 * list structure as line breaks, and decodes named and numeric character references.
 */
public final class HtmlText {

    private static final Pattern ENTITY = Pattern.compile("&(#\\d{1,8}|#[xX][0-9a-fA-F]{1,7}|[A-Za-z][A-Za-z0-9]{1,15});");
    private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style)\\b.*?</\\1\\s*>");
    private static final Pattern COMMENT = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern LIST_ITEM = Pattern.compile("(?i)<\\s*li\\b[^>]*>");
    private static final Pattern BLOCK_END = Pattern.compile("(?i)<\\s*(?:br|/p|/div|/ul|/ol|/h[1-6]|/tr)\\b[^>]*>");
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern HORIZONTAL_SPACE = Pattern.compile("[ \\t\\f\\u00A0\\u3000]+");
    private static final Pattern EXTRA_BLANK_LINES = Pattern.compile("\\n{3,}");

    private static final Map<String, String> NAMED = Map.ofEntries(
            Map.entry("amp", "&"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("quot", "\""), Map.entry("apos", "'"), Map.entry("nbsp", " "),
            Map.entry("ndash", "–"), Map.entry("mdash", "—"), Map.entry("hellip", "…"),
            Map.entry("lsquo", "‘"), Map.entry("rsquo", "’"),
            Map.entry("ldquo", "“"), Map.entry("rdquo", "”"),
            Map.entry("middot", "·"), Map.entry("bull", "•"),
            Map.entry("copy", "©"), Map.entry("reg", "®"), Map.entry("trade", "™"));

    private HtmlText() {
    }

    /** Converts an HTML fragment (escaped or not) to plain text with paragraphs kept. */
    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String s = html;
        if (s.indexOf('<') < 0 && s.contains("&lt;")) {
            s = unescape(s); // the whole document was escaped once more by the exporter
        }
        s = SCRIPT_STYLE.matcher(s).replaceAll(" ");
        s = COMMENT.matcher(s).replaceAll(" ");
        s = LIST_ITEM.matcher(s).replaceAll("\n- ");
        s = BLOCK_END.matcher(s).replaceAll("\n");
        s = TAG.matcher(s).replaceAll("");
        s = unescape(s); // after tag removal, so an escaped "&lt;b&gt;" stays visible text
        return normalizeWhitespace(s);
    }

    /** Decodes character references; unknown names are left as written. */
    public static String unescape(String s) {
        Matcher m = ENTITY.matcher(s);
        StringBuilder out = new StringBuilder(s.length());
        while (m.find()) {
            String decoded = decode(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(decoded == null ? m.group() : decoded));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String decode(String ref) {
        if (ref.charAt(0) != '#') {
            return NAMED.get(ref);
        }
        boolean hex = ref.length() > 1 && (ref.charAt(1) == 'x' || ref.charAt(1) == 'X');
        int cp;
        try {
            cp = Integer.parseInt(ref.substring(hex ? 2 : 1), hex ? 16 : 10);
        } catch (NumberFormatException e) {
            return "�";
        }
        if (cp == 0 || cp > Character.MAX_CODE_POINT || (cp >= 0xD800 && cp <= 0xDFFF)) {
            return "�";
        }
        return Character.toString(cp);
    }

    private static String normalizeWhitespace(String s) {
        String[] lines = s.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder out = new StringBuilder(s.length());
        for (String line : lines) {
            out.append(HORIZONTAL_SPACE.matcher(line).replaceAll(" ").strip()).append('\n');
        }
        return EXTRA_BLANK_LINES.matcher(out).replaceAll("\n\n").strip();
    }
}
