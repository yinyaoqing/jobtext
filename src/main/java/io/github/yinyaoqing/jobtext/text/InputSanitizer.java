package io.github.yinyaoqing.jobtext.text;

import java.util.regex.Pattern;

/** Cleans untrusted text cells before they are parsed or written into reports. */
public final class InputSanitizer {

    /** C0/C1 control characters other than newline and tab. */
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    /** Zero-width characters and bidirectional overrides, which can hide or reorder text. */
    private static final Pattern INVISIBLE = Pattern.compile("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private InputSanitizer() {
    }

    /** Removes control and invisible characters; keeps line breaks. */
    public static String multiLine(String s) {
        if (s == null) {
            return "";
        }
        String noCr = s.replace("\r\n", "\n").replace('\r', '\n');
        return INVISIBLE.matcher(CONTROL.matcher(noCr).replaceAll("")).replaceAll("").strip();
    }

    /** As {@link #multiLine} and also folds every whitespace run into one space. */
    public static String singleLine(String s) {
        return WHITESPACE_RUN.matcher(multiLine(s)).replaceAll(" ");
    }
}
