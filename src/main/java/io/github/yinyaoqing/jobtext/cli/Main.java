package io.github.yinyaoqing.jobtext.cli;

import io.github.yinyaoqing.jobtext.csv.CsvFormatException;
import io.github.yinyaoqing.jobtext.posting.Analysis;
import io.github.yinyaoqing.jobtext.posting.PostingCsv;
import io.github.yinyaoqing.jobtext.posting.RowError;
import io.github.yinyaoqing.jobtext.report.MarkdownReport;
import io.github.yinyaoqing.jobtext.report.SummaryCsv;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;

/** Command-line entry point. */
public final class Main {

    static final int OK = 0;
    static final int ROWS_SKIPPED = 1;
    static final int BAD_ARGS = 2;
    static final int IO_ERROR = 3;

    private Main() {
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out, System.err));
    }

    static int run(String[] argv, PrintStream out, PrintStream err) {
        Args args;
        try {
            args = Args.parse(argv);
        } catch (Args.UsageException e) {
            err.println("error: " + e.getMessage());
            err.println();
            err.println(Args.USAGE);
            return BAD_ARGS;
        }
        if (args.help()) {
            out.println(Args.USAGE);
            return OK;
        }
        try {
            String text = TextFiles.read(args.in(), args.encoding(), args.maxBytes());
            PostingCsv.LoadResult loaded = PostingCsv.read(new StringReader(text));
            List<Analysis> analyses = loaded.postings().stream().map(Analysis::of).toList();

            String source = args.in().getFileName().toString();
            TextFiles.writeAtomically(args.out(), MarkdownReport.render(analyses, loaded.errors(), source));
            if (args.csvOut() != null) {
                TextFiles.writeAtomically(args.csvOut(), SummaryCsv.render(analyses));
            }

            for (RowError e : loaded.errors()) {
                err.println("skipped line " + e.line() + ": " + e.message());
            }
            out.println("analysed " + analyses.size() + " postings, skipped " + loaded.errors().size()
                    + " rows -> " + args.out());
            return args.strict() && !loaded.errors().isEmpty() ? ROWS_SKIPPED : OK;
        } catch (CsvFormatException e) {
            err.println("error: " + args.in() + " line " + e.line() + ": " + e.getMessage());
            return IO_ERROR;
        } catch (TextFiles.InputException e) {
            err.println("error: " + e.getMessage());
            return IO_ERROR;
        } catch (IOException e) {
            err.println("error: " + e);
            return IO_ERROR;
        }
    }
}
