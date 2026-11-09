package io.github.yinyaoqing.jobtext.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private Path input(String content, Charset charset) throws IOException {
        Path in = dir.resolve("postings.csv");
        Files.writeString(in, content, charset);
        return in;
    }

    private static final String CSV = """
            id,title,company,salary,description
            p1,前端工程師,山嵐軟體,月薪60000元,"<p>React、TypeScript，部分遠端</p>"
            p2,,缺職稱,,x
            """;

    @Test
    void writesReportAndCsv() throws IOException {
        Path in = input(CSV, StandardCharsets.UTF_8);
        Path md = dir.resolve("out/report.md");
        Path csv = dir.resolve("out/summary.csv");

        assertEquals(Main.OK, run("--in", in.toString(), "--out", md.toString(), "--csv=" + csv));
        String report = Files.readString(md, StandardCharsets.UTF_8);
        assertTrue(report.contains("| p1 | 山嵐軟體 | 前端工程師 | 60,000 /月 | 部分遠端 |"));
        assertTrue(Files.readString(csv).startsWith("id,company,title"));
        assertTrue(stderr().contains("skipped line 3: title is empty"));
        try (var leftovers = Files.list(md.getParent())) {
            assertEquals(2, leftovers.count(), "no temporary files left behind");
        }
    }

    @Test
    void strictFailsWhenRowsWereSkipped() throws IOException {
        Path in = input(CSV, StandardCharsets.UTF_8);
        assertEquals(Main.ROWS_SKIPPED, run("--in", in.toString(), "--out", dir.resolve("r.md").toString(), "--strict"));
    }

    @Test
    void readsBig5WhenAsked() throws IOException {
        Path in = input(CSV, Charset.forName("Big5"));
        Path md = dir.resolve("r.md");
        assertEquals(Main.IO_ERROR, run("--in", in.toString(), "--out", md.toString()));
        assertTrue(stderr().contains("--encoding Big5"));
        assertFalse(Files.exists(md));

        assertEquals(Main.OK, run("--in", in.toString(), "--out", md.toString(), "--encoding", "Big5"));
        assertTrue(Files.readString(md).contains("山嵐軟體"));
    }

    @Test
    void refusesOversizedInput() throws IOException {
        Path in = input(CSV, StandardCharsets.UTF_8);
        assertEquals(Main.IO_ERROR, run("--in", in.toString(), "--out", dir.resolve("r.md").toString(), "--max-bytes", "10"));
        assertTrue(stderr().contains("over the limit of 10"));
    }

    @Test
    void reportsBrokenCsvWithLine() throws IOException {
        Path in = input("id,title,company,description\np,T,C,\"never closed\n", StandardCharsets.UTF_8);
        assertEquals(Main.IO_ERROR, run("--in", in.toString(), "--out", dir.resolve("r.md").toString()));
        assertTrue(stderr().contains("line 2: unterminated quoted field"));
    }

    @Test
    void argumentErrors() throws IOException {
        Path in = input(CSV, StandardCharsets.UTF_8);
        assertEquals(Main.BAD_ARGS, run());
        assertEquals(Main.BAD_ARGS, run("--in", in.toString()));
        assertEquals(Main.BAD_ARGS, run("--in", in.toString(), "--out", in.toString()));
        assertEquals(Main.BAD_ARGS, run("--in", in.toString(), "--out", "a.md", "--csv", "a.md"));
        assertEquals(Main.BAD_ARGS, run("--in", in.toString(), "--out", "a.md", "--max-bytes", "lots"));
        assertEquals(Main.BAD_ARGS, run("--in", in.toString(), "--out", "a.md", "--encoding", "klingon"));
        assertEquals(Main.BAD_ARGS, run("--in", "--out", "a.md"));
        assertEquals(Main.BAD_ARGS, run("--nope"));
        assertTrue(stderr().contains("unknown option '--nope'"));
    }

    @Test
    void missingInputFile() {
        assertEquals(Main.IO_ERROR, run("--in", dir.resolve("none.csv").toString(), "--out", dir.resolve("r.md").toString()));
    }

    @Test
    void help() {
        assertEquals(Main.OK, run("--help"));
        assertTrue(out.toString(StandardCharsets.UTF_8).startsWith("Usage: jobtext"));
    }
}
