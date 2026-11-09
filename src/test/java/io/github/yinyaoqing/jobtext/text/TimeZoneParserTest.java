package io.github.yinyaoqing.jobtext.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimeZoneParserTest {

    private static List<ZoneOffset> offsets(String text) {
        return TimeZoneParser.parse(text).offsets();
    }

    @Test
    void readsExplicitOffsetsInTextOrder() {
        assertEquals(List.of(ZoneOffset.ofHours(-5), ZoneOffset.ofHours(1)), offsets("Overlap UTC-5 to UTC+1"));
        assertEquals(List.of(ZoneOffset.ofHoursMinutes(5, 30)), offsets("GMT+05:30"));
        assertEquals(List.of(ZoneOffset.ofHours(8)), offsets("台灣UTC+8"));
        assertEquals(List.of(ZoneOffset.ofHours(-3)), offsets("UTC−3"), "Unicode minus sign");
    }

    @Test
    void doesNotCountTheUtcOfAnOffsetTwice() {
        assertEquals(List.of(ZoneOffset.ofHours(8)), offsets("UTC+8"));
    }

    @Test
    void resolvesAbbreviationsAndChineseNames() {
        assertEquals(List.of(ZoneOffset.ofHours(-8), ZoneOffset.ofHours(1), ZoneOffset.ofHours(8)),
                offsets("PST mornings, CET afternoons, 台灣時間 evenings"));
    }

    @Test
    void ambiguousAbbreviationsAreNotGuessed() {
        TimeZoneParser.Mentions m = TimeZoneParser.parse("Core hours 9-5 CST, IST welcome");
        assertEquals(List.of(), m.offsets());
        assertEquals(List.of("CST", "IST"), m.ambiguous());
    }

    @Test
    void ignoresLookalikesAndOutOfRangeOffsets() {
        assertEquals(List.of(), offsets("BESTSELLER at the PSTN, UTC+20"));
    }

    @Test
    void deduplicates() {
        assertEquals(List.of(ZoneOffset.ofHours(8)), offsets("UTC+8 / 台灣時間 / SGT"));
    }
}
