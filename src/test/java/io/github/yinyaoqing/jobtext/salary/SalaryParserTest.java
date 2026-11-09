package io.github.yinyaoqing.jobtext.salary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yinyaoqing.jobtext.salary.Salary.Period;
import io.github.yinyaoqing.jobtext.salary.Salary.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SalaryParserTest {

    private static Range range(String text) {
        return assertInstanceOf(Range.class, SalaryParser.parse(text).orElseThrow(), text);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            月薪65,000~125,000元         | MONTH | 65000   | 125000  | false
            月薪 4萬 以上                 | MONTH | 40000   | 40000   | true
            年薪80~120萬                 | YEAR  | 800000  | 1200000 | false
            年薪 1.2萬至1.5萬            | YEAR  | 12000   | 15000   | false
            時薪200元                    | HOUR  | 200     | 200     | false
            日薪：NT$2,000              | DAY   | 2000    | 2000    | false
            工作待遇 月薪50,000-70,000元  | MONTH | 50000   | 70000   | false
            """)
    void parsesTaiwaneseFormats(String text, Period period, long min, long max, boolean open) {
        Range r = range(text);
        assertEquals(new Range(period, min, max, "TWD", open), r);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            US$120k–150k / year        | YEAR  | 120000 | 150000 | USD
            $120-150k                  | YEAR  | 120000 | 150000 | USD
            €60,000 per year           | YEAR  | 60000  | 60000  | EUR
            £4,500 a month             | MONTH | 4500   | 4500   | GBP
            $45/hr                     | HOUR  | 45     | 45     | USD
            NT$1.5M / year             | YEAR  | 1500000 | 1500000 | TWD
            NT$90000/month             | MONTH | 90000  | 90000  | TWD
            $100 monthly bonus, $70k   | HOUR  | 100    | 100    | USD
            """)
    void parsesEnglishFormats(String text, Period period, long min, long max, String currency) {
        Range r = range(text);
        assertEquals(period, r.period());
        assertEquals(min, r.min());
        assertEquals(max, r.max());
        assertEquals(currency, r.currency());
    }

    @Test
    void negotiableKeepsTheLegalFloor() {
        Salary s = SalaryParser.parse("待遇面議（經常性薪資達4萬元或以上）").orElseThrow();
        Salary.Negotiable n = assertInstanceOf(Salary.Negotiable.class, s);
        assertEquals(new Range(Period.MONTH, 40000, 40000, "TWD", true), n.floor().orElseThrow());
    }

    @Test
    void negotiableWithoutFloor() {
        Salary.Negotiable n = assertInstanceOf(Salary.Negotiable.class, SalaryParser.parse("待遇面議").orElseThrow());
        assertTrue(n.floor().isEmpty());
    }

    @Test
    void ignoresNumbersThatAreNotSalaries() {
        assertTrue(SalaryParser.parse("3年以上經驗，需求人數2~3人").isEmpty());
        assertTrue(SalaryParser.parse("").isEmpty());
        assertTrue(SalaryParser.parse(null).isEmpty());
    }

    @Test
    void skipsInvertedRangeAndTriesTheNextMatch() {
        assertEquals(50000, range("月薪90,000~10,000元；月薪50,000元").min());
    }

    @Test
    void refusesAbsurdlyLongNumbers() {
        assertTrue(SalaryParser.parse("月薪1234567890123456789元").isEmpty());
    }

    @Test
    void monthlyEstimates() {
        assertEquals(100000, new Range(Period.YEAR, 1_200_000, 1_200_000, "TWD", false).monthlyMin());
        assertEquals(35200, new Range(Period.HOUR, 200, 200, "TWD", false).monthlyMin());
        assertEquals(44000, new Range(Period.DAY, 2000, 2000, "TWD", false).monthlyMin());
    }

    @Test
    void rangeRejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () -> new Range(Period.MONTH, 10, 5, "TWD", false));
        assertThrows(IllegalArgumentException.class, () -> new Range(Period.MONTH, -1, 5, "TWD", false));
    }
}
