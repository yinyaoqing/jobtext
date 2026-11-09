package io.github.yinyaoqing.jobtext.report;

import io.github.yinyaoqing.jobtext.csv.CsvWriter;
import io.github.yinyaoqing.jobtext.posting.Analysis;
import io.github.yinyaoqing.jobtext.salary.Salary;
import java.util.List;
import java.util.Locale;

/** One machine-readable row per posting, for spreadsheets. */
public final class SummaryCsv {

    static final List<String> HEADER = List.of("id", "company", "title", "salary_text", "salary_period",
            "salary_min", "salary_max", "currency", "monthly_min", "negotiable", "remote_mode", "office_days_per_week",
            "utc_offsets", "tech");

    private SummaryCsv() {
    }

    public static String render(List<Analysis> items) {
        StringBuilder out = new StringBuilder(CsvWriter.row(HEADER));
        for (Analysis a : items) {
            Salary.Range range = null;
            boolean negotiable = false;
            if (a.salary().isPresent()) {
                switch (a.salary().get()) {
                    case Salary.Range r -> range = r;
                    case Salary.Negotiable n -> {
                        negotiable = true;
                        range = n.floor().orElse(null);
                    }
                }
            }
            out.append(CsvWriter.row(List.of(
                    a.posting().id(),
                    a.posting().company(),
                    a.posting().title(),
                    Format.salary(a.salary()),
                    range == null ? "" : range.period().name().toLowerCase(Locale.ROOT),
                    range == null ? "" : Long.toString(range.min()),
                    range == null ? "" : Long.toString(range.max()),
                    range == null ? "" : range.currency(),
                    range == null ? "" : Long.toString(range.monthlyMin()),
                    Boolean.toString(negotiable),
                    a.remote().mode().name().toLowerCase(Locale.ROOT),
                    a.remote().officeDaysPerWeek().isPresent()
                            ? Integer.toString(a.remote().officeDaysPerWeek().getAsInt()) : "",
                    Format.offsets(a.timeZones().offsets()),
                    String.join("; ", a.tech()))));
        }
        return out.toString();
    }
}
