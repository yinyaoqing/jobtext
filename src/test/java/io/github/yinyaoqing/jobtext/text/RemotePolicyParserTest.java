package io.github.yinyaoqing.jobtext.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.yinyaoqing.jobtext.text.RemotePolicy.Mode;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RemotePolicyParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            遠端工作：部分遠端，若在不同縣市,一週進公司開一次會即可 | HYBRID      | 1
            每週需到辦公室兩天                                    | HYBRID      | 2
            Hybrid, 3 days a week in the office                   | HYBRID      | 3
            two days per week at office                           | HYBRID      | 2
            完全遠端，台灣時間上班                                | FULL_REMOTE | 0
            We are a remote-first company                         | FULL_REMOTE | 0
            此職務不可遠端                                        | ONSITE      | 0
            On-site only in Taipei                                | ONSITE      | 0
            週休二日，年終獎金                                    | UNKNOWN     | 0
            """)
    void classifies(String text, Mode mode, int days) {
        RemotePolicy p = RemotePolicyParser.parse(text);
        assertEquals(mode, p.mode());
        assertEquals(days == 0 ? OptionalInt.empty() : OptionalInt.of(days), p.officeDaysPerWeek());
    }

    @Test
    void onsiteWinsOverOtherWording() {
        assertEquals(Mode.ONSITE, RemotePolicyParser.parse("No remote. Hybrid schedules are not offered.").mode());
    }

    @Test
    void hybridIsAWholeWord() {
        assertEquals(Mode.UNKNOWN, RemotePolicyParser.parse("hybridization research").mode());
    }

    @Test
    void blankInput() {
        assertEquals(Mode.UNKNOWN, RemotePolicyParser.parse(null).mode());
    }
}
