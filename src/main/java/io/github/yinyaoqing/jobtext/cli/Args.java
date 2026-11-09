package io.github.yinyaoqing.jobtext.cli;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/** Validated command-line options. */
record Args(Path in, Path out, Path csvOut, Charset encoding, long maxBytes, boolean strict, boolean help) {

    static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024;
    static final long LIMIT_MAX_BYTES = 1024L * 1024 * 1024;

    static final String USAGE = """
            Usage: jobtext --in <postings.csv> --out <report.md> [options]

              --in <file>          CSV with columns id,title,company,description
                                   (optional: location,salary,remote)
              --out <file>         Markdown report to write
              --csv <file>         also write a one-row-per-posting CSV summary
              --encoding <name>    input encoding (default UTF-8; e.g. Big5 for old Excel exports)
              --max-bytes <n>      refuse inputs larger than n bytes (default 10485760)
              --strict             exit 1 when any row was skipped
              -h, --help           show this help

            Exit codes: 0 ok, 1 rows skipped with --strict, 2 bad arguments, 3 input or output error""";

    static final class UsageException extends Exception {

        private static final long serialVersionUID = 1L;

        UsageException(String message) {
            super(message);
        }
    }

    static Args parse(String[] argv) throws UsageException {
        Path in = null;
        Path out = null;
        Path csvOut = null;
        Charset encoding = StandardCharsets.UTF_8;
        long maxBytes = DEFAULT_MAX_BYTES;
        boolean strict = false;

        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            String name = arg;
            String inline = null;
            int eq = arg.indexOf('=');
            if (arg.startsWith("--") && eq > 0) {
                name = arg.substring(0, eq);
                inline = arg.substring(eq + 1);
            }
            switch (name) {
                case "-h", "--help" -> {
                    return new Args(null, null, null, encoding, maxBytes, false, true);
                }
                case "--strict" -> strict = true;
                case "--in", "--out", "--csv", "--encoding", "--max-bytes" -> {
                    String value = inline;
                    if (value == null) {
                        if (i + 1 >= argv.length || argv[i + 1].startsWith("--")) {
                            throw new UsageException(name + " needs a value");
                        }
                        value = argv[++i];
                    }
                    if (value.isBlank()) {
                        throw new UsageException(name + " needs a value");
                    }
                    switch (name) {
                        case "--in" -> in = path(name, value);
                        case "--out" -> out = path(name, value);
                        case "--csv" -> csvOut = path(name, value);
                        case "--encoding" -> encoding = charset(value);
                        default -> maxBytes = maxBytes(value);
                    }
                }
                default -> throw new UsageException("unknown option '" + arg + "'");
            }
        }
        if (in == null) {
            throw new UsageException("--in is required");
        }
        if (out == null) {
            throw new UsageException("--out is required");
        }
        if (same(in, out) || (csvOut != null && same(in, csvOut))) {
            throw new UsageException("an output file must not be the input file");
        }
        if (csvOut != null && same(out, csvOut)) {
            throw new UsageException("--out and --csv must be different files");
        }
        return new Args(in, out, csvOut, encoding, maxBytes, strict, false);
    }

    private static Path path(String option, String value) throws UsageException {
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new UsageException(option + ": invalid path '" + value + "'");
        }
    }

    private static Charset charset(String value) throws UsageException {
        try {
            return Charset.forName(value);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UsageException("unknown encoding '" + value + "'");
        }
    }

    private static long maxBytes(String value) throws UsageException {
        long n;
        try {
            n = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new UsageException("--max-bytes must be a whole number, got '" + value + "'");
        }
        if (n <= 0 || n > LIMIT_MAX_BYTES) {
            throw new UsageException("--max-bytes must be between 1 and " + LIMIT_MAX_BYTES);
        }
        return n;
    }

    private static boolean same(Path a, Path b) {
        return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
    }
}
