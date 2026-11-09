package io.github.yinyaoqing.jobtext.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvReaderTest {

    private static List<CsvReader.Row> readAll(String csv) throws IOException {
        CsvReader reader = new CsvReader(new StringReader(csv));
        List<CsvReader.Row> rows = new ArrayList<>();
        for (CsvReader.Row r = reader.next(); r != null; r = reader.next()) {
            rows.add(r);
        }
        return rows;
    }

    @Test
    void readsPlainRowsWithAnyLineEnding() throws IOException {
        List<CsvReader.Row> rows = readAll("a,b\r\nc,d\ne,f\rg,h");
        assertEquals(4, rows.size());
        assertEquals(List.of("a", "b"), rows.get(0).fields());
        assertEquals(List.of("g", "h"), rows.get(3).fields());
        assertEquals(4, rows.get(3).line());
    }

    @Test
    void quotedFieldsKeepCommasQuotesAndLineBreaks() throws IOException {
        List<CsvReader.Row> rows = readAll("id,text\n1,\"he said \"\"hi\"\", then\r\nleft\"\n2,x\n");
        assertEquals(List.of("1", "he said \"hi\", then\nleft"), rows.get(1).fields());
        assertEquals(2, rows.get(1).line());
        assertEquals(4, rows.get(2).line(), "line numbers count the break inside the quoted field");
    }

    @Test
    void keepsEmptyFieldsAndTrailingComma() throws IOException {
        assertEquals(List.of("", "b", ""), readAll(",b,\n").get(0).fields());
        assertEquals(List.of("", ""), readAll("\"\",\"\"").get(0).fields());
    }

    @Test
    void stripsByteOrderMark() throws IOException {
        assertEquals(List.of("id", "title"), readAll("﻿id,title\n").get(0).fields());
    }

    @Test
    void reportsBlankLines() throws IOException {
        List<CsvReader.Row> rows = readAll("a\n\nb\n");
        assertTrue(rows.get(1).isBlank());
        assertEquals(3, rows.size());
    }

    @Test
    void emptyInputHasNoRows() throws IOException {
        assertNull(new CsvReader(new StringReader("")).next());
    }

    @Test
    void unterminatedQuoteNamesTheStartLine() {
        CsvFormatException e = assertThrows(CsvFormatException.class, () -> readAll("a\n\"open\nstill open"));
        assertEquals(2, e.line());
    }

    @Test
    void rejectsQuoteInsideUnquotedField() {
        CsvFormatException e = assertThrows(CsvFormatException.class, () -> readAll("ok\nab\"c\n"));
        assertEquals(2, e.line());
    }

    @Test
    void rejectsTextAfterClosingQuote() {
        assertThrows(CsvFormatException.class, () -> readAll("\"a\"b,c\n"));
    }
}
