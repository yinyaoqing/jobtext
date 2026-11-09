package io.github.yinyaoqing.jobtext.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yinyaoqing.jobtext.csv.CsvReader;
import io.github.yinyaoqing.jobtext.posting.Analysis;
import io.github.yinyaoqing.jobtext.posting.Posting;
import io.github.yinyaoqing.jobtext.posting.RowError;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportTest {

    private static Analysis analysis(String id, String company, String salary, String description) {
        return Analysis.of(new Posting(id, "Frontend Engineer", company, "", salary, "", description));
    }

    @Test
    void cellEscapingKeepsTheTableIntact() {
        assertEquals("a \\| b &lt;script&gt; c", MarkdownReport.cell("a | b <script>\nc"));
        assertEquals("—", MarkdownReport.cell("  "));
        assertEquals("C:\\\\path", MarkdownReport.cell("C:\\path"));
    }

    @Test
    void reportHasSummaryRowsAndErrors() {
        List<Analysis> items = List.of(
                analysis("a", "Acme | Co", "月薪60,000~80,000元", "<p>React, TypeScript. 部分遠端，每週進公司一天</p>"),
                analysis("b", "Beta", "", "<p>React. Fully remote, UTC+8. 月薪100,000元</p>"),
                analysis("c", "Gamma", "待遇面議", "Vue"));
        String md = MarkdownReport.render(items, List.of(new RowError(9, "title is empty")), "jobs.csv");

        assertTrue(md.contains("職缺 3 筆 · 略過 1 列"));
        assertTrue(md.contains("完全遠端 1、部分遠端 1、到辦公室 0、未註明 1"));
        assertTrue(md.contains("2 筆，中位數 80,000，範圍 60,000–100,000"));
        assertTrue(md.contains("React (2)"));
        assertTrue(md.contains("| a | Acme \\| Co | Frontend Engineer | 60,000–80,000 /月 | 部分遠端 | 1 |"));
        assertTrue(md.contains("| UTC+08:00 |"));
        assertTrue(md.contains("| c | Gamma | Frontend Engineer | 面議 |"));
        assertTrue(md.contains("| 9 | title is empty |"));
    }

    @Test
    void reportWithoutErrorsHasNoErrorSection() {
        assertFalse(MarkdownReport.render(List.of(), List.of(), "x.csv").contains("略過的列"));
    }

    @Test
    void summaryCsvIsParseableAndSafe() throws IOException {
        List<Analysis> items = List.of(
                analysis("a", "=cmd|' /C calc'!A0", "待遇面議（經常性薪資達4萬元或以上）", "Java, React"));
        String csv = SummaryCsv.render(items);
        CsvReader reader = new CsvReader(new StringReader(csv));
        assertEquals(SummaryCsv.HEADER, reader.next().fields());
        List<String> row = reader.next().fields();
        assertEquals("'=cmd|' /C calc'!A0", row.get(1));
        assertEquals("month", row.get(4));
        assertEquals("40000", row.get(8));
        assertEquals("true", row.get(9));
        assertEquals("Java; React", row.get(13));
    }
}
