package io.github.yinyaoqing.jobtext.csv;

import java.util.List;
import java.util.stream.Collectors;

/** Writes RFC 4180 rows and neutralises spreadsheet formulas in untrusted cells. */
public final class CsvWriter {

    private CsvWriter() {
    }

    /** Formats one row, ending with CRLF as RFC 4180 specifies. */
    public static String row(List<String> fields) {
        return fields.stream().map(CsvWriter::cell).collect(Collectors.joining(",")) + "\r\n";
    }

    /**
     * Quotes a cell when needed. A cell starting with {@code = + - @}, tab or CR is prefixed with
     * an apostrophe so Excel or Sheets shows it as text instead of running it as a formula
     * (CSV injection).
     */
    public static String cell(String value) {
        String s = value == null ? "" : value;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
