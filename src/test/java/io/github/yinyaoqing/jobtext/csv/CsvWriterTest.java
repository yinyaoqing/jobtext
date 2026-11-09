package io.github.yinyaoqing.jobtext.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void quotesOnlyWhenNeeded() {
        assertEquals("plain", CsvWriter.cell("plain"));
        assertEquals("\"a,b\"", CsvWriter.cell("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvWriter.cell("say \"hi\""));
        assertEquals("\"two\nlines\"", CsvWriter.cell("two\nlines"));
        assertEquals("", CsvWriter.cell(null));
    }

    @Test
    void neutralisesSpreadsheetFormulas() {
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", CsvWriter.cell("=HYPERLINK(\"x\")"));
        assertEquals("'+1", CsvWriter.cell("+1"));
        assertEquals("'@SUM(A1)", CsvWriter.cell("@SUM(A1)"));
        assertEquals("'-2", CsvWriter.cell("-2"));
    }

    @Test
    void roundTripsThroughTheReader() throws IOException {
        List<String> fields = List.of("1", "React, \"TS\"", "line1\nline2", "");
        CsvReader.Row row = new CsvReader(new StringReader(CsvWriter.row(fields))).next();
        assertEquals(fields, row.fields());
    }
}
