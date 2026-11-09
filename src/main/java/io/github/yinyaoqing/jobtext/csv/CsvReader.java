package io.github.yinyaoqing.jobtext.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Streaming RFC 4180 reader written as a small state machine: quoted fields, doubled quotes,
 * commas and line breaks inside quotes, CRLF / LF / CR line endings and a leading BOM.
 *
 * <p>Each row carries the physical line it started on, so callers can report errors the way a
 * person sees the file in an editor even when a quoted field spans several lines.
 */
public final class CsvReader {

    /** One record and the 1-based line it starts on. */
    public record Row(long line, List<String> fields) {

        /** True for an empty line, which RFC 4180 readers see as a single empty field. */
        public boolean isBlank() {
            return fields.size() == 1 && fields.get(0).isBlank();
        }
    }

    private static final int NONE = -2;

    private final Reader in;
    private int peeked = NONE;
    private long line = 1;
    private boolean started;

    public CsvReader(Reader reader) {
        this.in = reader instanceof BufferedReader ? reader : new BufferedReader(reader);
    }

    /** Returns the next row, or {@code null} at end of input. */
    public Row next() throws IOException {
        if (!started) {
            started = true;
            if (peek() == '﻿') {
                read();
            }
        }
        if (peek() == -1) {
            return null;
        }
        long startLine = line;
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean closedQuote = false;
        boolean fieldStart = true;

        while (true) {
            int c = read();
            if (inQuotes) {
                switch (c) {
                    case -1 -> throw new CsvFormatException(startLine, "unterminated quoted field");
                    case '"' -> {
                        if (peek() == '"') {
                            read();
                            field.append('"');
                        } else {
                            inQuotes = false;
                            closedQuote = true;
                        }
                    }
                    case '\r' -> {
                        if (peek() == '\n') {
                            read();
                        }
                        line++;
                        field.append('\n');
                    }
                    case '\n' -> {
                        line++;
                        field.append('\n');
                    }
                    default -> field.append((char) c);
                }
                continue;
            }
            if (c == -1 || c == '\n' || c == '\r') {
                if (c == '\r' && peek() == '\n') {
                    read();
                }
                if (c != -1) {
                    line++;
                }
                fields.add(field.toString());
                return new Row(startLine, List.copyOf(fields));
            }
            if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                fieldStart = true;
                closedQuote = false;
                continue;
            }
            if (closedQuote) {
                throw new CsvFormatException(line, "unexpected character after a closing quote");
            }
            if (c == '"') {
                if (!fieldStart) {
                    throw new CsvFormatException(line, "quote inside an unquoted field");
                }
                inQuotes = true;
                fieldStart = false;
                continue;
            }
            field.append((char) c);
            fieldStart = false;
        }
    }

    private int read() throws IOException {
        if (peeked != NONE) {
            int c = peeked;
            peeked = NONE;
            return c;
        }
        return in.read();
    }

    private int peek() throws IOException {
        if (peeked == NONE) {
            peeked = in.read();
        }
        return peeked;
    }
}
