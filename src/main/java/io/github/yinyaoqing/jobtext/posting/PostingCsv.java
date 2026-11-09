package io.github.yinyaoqing.jobtext.posting;

import io.github.yinyaoqing.jobtext.csv.CsvFormatException;
import io.github.yinyaoqing.jobtext.csv.CsvReader;
import io.github.yinyaoqing.jobtext.text.InputSanitizer;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Maps CSV rows to {@link Posting}s and validates them. A bad row is skipped and reported with
 * its line number; only a problem with the file as a whole (bad header, broken quoting) stops
 * the run.
 */
public final class PostingCsv {

    public static final int MAX_FIELD_CHARS = 50_000;
    public static final int MAX_ROWS = 100_000;

    private static final List<String> REQUIRED = List.of("id", "title", "company", "description");
    private static final List<String> OPTIONAL = List.of("location", "salary", "remote");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** Valid postings in file order, and the rows that were skipped. */
    public record LoadResult(List<Posting> postings, List<RowError> errors) {

        public LoadResult {
            postings = List.copyOf(postings);
            errors = List.copyOf(errors);
        }
    }

    private PostingCsv() {
    }

    public static LoadResult read(Reader reader) throws IOException {
        CsvReader csv = new CsvReader(reader);
        CsvReader.Row header = csv.next();
        while (header != null && header.isBlank()) {
            header = csv.next();
        }
        if (header == null) {
            throw new CsvFormatException(1, "the file is empty; expected a header row");
        }
        Map<String, Integer> columns = columns(header);

        List<Posting> postings = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Map<String, Long> seenIds = new HashMap<>();
        int rows = 0;
        for (CsvReader.Row row = csv.next(); row != null; row = csv.next()) {
            if (row.isBlank()) {
                continue;
            }
            if (++rows > MAX_ROWS) {
                throw new CsvFormatException(row.line(), "more than " + MAX_ROWS + " rows");
            }
            if (row.fields().size() != header.fields().size()) {
                errors.add(new RowError(row.line(), "expected " + header.fields().size() + " fields, found "
                        + row.fields().size()));
                continue;
            }
            String tooLong = firstTooLong(row, columns);
            if (tooLong != null) {
                errors.add(new RowError(row.line(), "field '" + tooLong + "' is longer than " + MAX_FIELD_CHARS
                        + " characters"));
                continue;
            }
            Posting posting = new Posting(
                    single(row, columns, "id"),
                    single(row, columns, "title"),
                    single(row, columns, "company"),
                    single(row, columns, "location"),
                    single(row, columns, "salary"),
                    single(row, columns, "remote"),
                    InputSanitizer.multiLine(raw(row, columns, "description")));
            String problem = validate(posting);
            if (problem != null) {
                errors.add(new RowError(row.line(), problem));
                continue;
            }
            Long firstLine = seenIds.putIfAbsent(posting.id(), row.line());
            if (firstLine != null) {
                errors.add(new RowError(row.line(), "duplicate id '" + posting.id() + "' (first seen on line "
                        + firstLine + ")"));
                continue;
            }
            postings.add(posting);
        }
        return new LoadResult(postings, errors);
    }

    private static Map<String, Integer> columns(CsvReader.Row header) throws CsvFormatException {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i < header.fields().size(); i++) {
            String name = InputSanitizer.singleLine(header.fields().get(i)).toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue; // unnamed extra column, ignored
            }
            if (columns.putIfAbsent(name, i) != null) {
                throw new CsvFormatException(header.line(), "duplicate column '" + name + "'");
            }
        }
        List<String> missing = REQUIRED.stream().filter(c -> !columns.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new CsvFormatException(header.line(), "missing required column(s): " + String.join(", ", missing)
                    + " (optional: " + String.join(", ", OPTIONAL) + ")");
        }
        return columns;
    }

    private static String firstTooLong(CsvReader.Row row, Map<String, Integer> columns) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (row.fields().get(column.getValue()).length() > MAX_FIELD_CHARS) {
                return column.getKey();
            }
        }
        return null;
    }

    private static String validate(Posting p) {
        if (p.id().isEmpty()) {
            return "id is empty";
        }
        if (!ID.matcher(p.id()).matches()) {
            return "id '" + p.id() + "' may only use letters, digits, '.', '_' and '-' (max 64)";
        }
        if (p.title().isEmpty()) {
            return "title is empty";
        }
        if (p.company().isEmpty()) {
            return "company is empty";
        }
        return null;
    }

    private static String raw(CsvReader.Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null ? "" : row.fields().get(index);
    }

    private static String single(CsvReader.Row row, Map<String, Integer> columns, String name) {
        return InputSanitizer.singleLine(raw(row, columns, name));
    }
}
