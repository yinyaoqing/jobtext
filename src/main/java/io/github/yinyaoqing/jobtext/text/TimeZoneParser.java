package io.github.yinyaoqing.jobtext.text;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects the UTC offsets a posting mentions: explicit offsets ("UTC+8", "GMT-05:00"),
 * zone abbreviations ("PST", "CET") and Chinese names ("台灣時間"). Abbreviations that mean
 * several zones (CST, IST) are reported as ambiguous instead of guessed.
 */
public final class TimeZoneParser {

    /** Offsets in text order and abbreviations that could not be resolved. */
    public record Mentions(List<ZoneOffset> offsets, List<String> ambiguous) {

        public Mentions {
            offsets = List.copyOf(offsets);
            ambiguous = List.copyOf(ambiguous);
        }
    }

    private static final Pattern OFFSET = Pattern.compile(
            "(?i)\\b(?:UTC|GMT)\\s*(?<sign>[+\\-−–])\\s*(?<h>\\d{1,2})(?::?(?<m>[0-5]\\d))?");
    private static final Pattern ABBREVIATION = Pattern.compile(
            "(?<![A-Za-z])(PST|PDT|MST|MDT|CDT|EST|EDT|BST|CET|CEST|EET|EEST|SGT|HKT|JST|KST|AEST|AEDT|UTC|GMT|CST|IST)(?![A-Za-z])");
    private static final Pattern CHINESE_NAME = Pattern.compile("台灣時間|臺灣時間|台北時間|日本時間|新加坡時間|香港時間");

    private static final Map<String, Integer> ABBREVIATION_HOURS = Map.ofEntries(
            Map.entry("PST", -8), Map.entry("PDT", -7), Map.entry("MST", -7), Map.entry("MDT", -6),
            Map.entry("CDT", -5), Map.entry("EST", -5), Map.entry("EDT", -4),
            Map.entry("UTC", 0), Map.entry("GMT", 0), Map.entry("BST", 1),
            Map.entry("CET", 1), Map.entry("CEST", 2), Map.entry("EET", 2), Map.entry("EEST", 3),
            Map.entry("SGT", 8), Map.entry("HKT", 8), Map.entry("JST", 9), Map.entry("KST", 9),
            Map.entry("AEST", 10), Map.entry("AEDT", 11));
    private static final Set<String> AMBIGUOUS = Set.of("CST", "IST");

    private static final int MAX_OFFSET_HOURS = 14;

    private record Hit(int position, ZoneOffset offset) {
    }

    private TimeZoneParser() {
    }

    public static Mentions parse(String text) {
        if (text == null || text.isBlank()) {
            return new Mentions(List.of(), List.of());
        }
        List<Hit> hits = new ArrayList<>();
        List<int[]> offsetSpans = new ArrayList<>();
        Matcher m = OFFSET.matcher(text);
        while (m.find()) {
            int hours = Integer.parseInt(m.group("h"));
            int minutes = m.group("m") == null ? 0 : Integer.parseInt(m.group("m"));
            offsetSpans.add(new int[] {m.start(), m.end()}); // even when invalid, so "UTC" is not re-read below
            if (hours > MAX_OFFSET_HOURS) {
                continue;
            }
            int sign = m.group("sign").equals("+") ? 1 : -1;
            hits.add(new Hit(m.start(), ZoneOffset.ofHoursMinutes(sign * hours, sign * minutes)));
        }
        Set<String> ambiguous = new LinkedHashSet<>();
        Matcher a = ABBREVIATION.matcher(text);
        while (a.find()) {
            if (inside(a.start(), offsetSpans)) {
                continue; // the "UTC" of "UTC+8" was already read as an offset
            }
            String abbreviation = a.group(1);
            if (AMBIGUOUS.contains(abbreviation)) {
                ambiguous.add(abbreviation);
            } else {
                hits.add(new Hit(a.start(), ZoneOffset.ofHours(ABBREVIATION_HOURS.get(abbreviation))));
            }
        }
        Matcher c = CHINESE_NAME.matcher(text);
        while (c.find()) {
            int hours = c.group().startsWith("日本") ? 9 : 8;
            hits.add(new Hit(c.start(), ZoneOffset.ofHours(hours)));
        }
        hits.sort(Comparator.comparingInt(Hit::position));
        Set<ZoneOffset> offsets = new LinkedHashSet<>();
        hits.forEach(h -> offsets.add(h.offset()));
        return new Mentions(new ArrayList<>(offsets), new ArrayList<>(ambiguous));
    }

    private static boolean inside(int position, List<int[]> spans) {
        for (int[] span : spans) {
            if (position >= span[0] && position < span[1]) {
                return true;
            }
        }
        return false;
    }
}
