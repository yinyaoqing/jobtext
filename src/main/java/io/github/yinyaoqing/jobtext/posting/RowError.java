package io.github.yinyaoqing.jobtext.posting;

/** A row that was skipped, with the 1-based line it starts on. */
public record RowError(long line, String message) {
}
