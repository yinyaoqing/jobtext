package io.github.yinyaoqing.jobtext.csv;

import java.io.IOException;

/** A structural CSV error that stops reading, such as an unterminated quote. */
public final class CsvFormatException extends IOException {

    private static final long serialVersionUID = 1L;

    private final long line;

    public CsvFormatException(long line, String message) {
        super(message);
        this.line = line;
    }

    /** 1-based physical line where the problem was found. */
    public long line() {
        return line;
    }
}
