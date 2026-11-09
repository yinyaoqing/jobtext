package io.github.yinyaoqing.jobtext.report;

import io.github.yinyaoqing.jobtext.posting.Analysis;
import io.github.yinyaoqing.jobtext.posting.RowError;
import io.github.yinyaoqing.jobtext.salary.Salary;
import io.github.yinyaoqing.jobtext.text.RemotePolicy;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Renders the analysis as a Markdown report with a summary, a postings table and skipped rows. */
public final class MarkdownReport {

    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");
    private static final int TOP_TECH = 10;

    private MarkdownReport() {
    }

    public static String render(List<Analysis> items, List<RowError> errors, String sourceName) {
        StringBuilder md = new StringBuilder();
        md.append("# 職缺文本分析報告\n\n");
        md.append("來源：").append(cell(sourceName)).append(" · 職缺 ").append(items.size())
                .append(" 筆 · 略過 ").append(errors.size()).append(" 列\n\n");

        md.append("## 摘要\n\n");
        md.append("- 遠端：").append(remoteSummary(items)).append('\n');
        md.append("- 月薪下限（TWD，有標示金額者）：").append(salarySummary(items)).append('\n');
        md.append("- 常見技術：").append(techSummary(items)).append("\n\n");

        md.append("## 職缺\n\n");
        md.append("| ID | 公司 | 職稱 | 薪資 | 遠端 | 進公司（天/週） | 時區 | 技術 |\n");
        md.append("|---|---|---|---|---|---|---|---|\n");
        for (Analysis a : items) {
            md.append("| ").append(cell(a.posting().id()))
                    .append(" | ").append(cell(a.posting().company()))
                    .append(" | ").append(cell(a.posting().title()))
                    .append(" | ").append(cell(Format.salary(a.salary())))
                    .append(" | ").append(Format.mode(a.remote().mode()))
                    .append(" | ").append(a.remote().officeDaysPerWeek().isPresent()
                            ? String.valueOf(a.remote().officeDaysPerWeek().getAsInt()) : "—")
                    .append(" | ").append(cell(zones(a)))
                    .append(" | ").append(cell(String.join(", ", a.tech())))
                    .append(" |\n");
        }

        if (!errors.isEmpty()) {
            md.append("\n## 略過的列\n\n| 行 | 原因 |\n|---|---|\n");
            for (RowError e : errors) {
                md.append("| ").append(e.line()).append(" | ").append(cell(e.message())).append(" |\n");
            }
        }
        return md.toString();
    }

    /**
     * Makes untrusted text safe inside a table cell: escapes backslashes and pipes, encodes
     * {@code & < >} so raw HTML is never rendered, and folds line breaks.
     */
    static String cell(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        String escaped = s.replace("\\", "\\\\").replace("|", "\\|")
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return WHITESPACE_RUN.matcher(escaped).replaceAll(" ").strip();
    }

    private static String zones(Analysis a) {
        String offsets = Format.offsets(a.timeZones().offsets());
        if (a.timeZones().ambiguous().isEmpty()) {
            return offsets;
        }
        String unclear = String.join(", ", a.timeZones().ambiguous()) + "（不明確）";
        return offsets.isEmpty() ? unclear : offsets + ", " + unclear;
    }

    private static String remoteSummary(List<Analysis> items) {
        Map<RemotePolicy.Mode, Long> counts = new EnumMap<>(RemotePolicy.Mode.class);
        for (RemotePolicy.Mode mode : RemotePolicy.Mode.values()) {
            counts.put(mode, 0L);
        }
        items.forEach(a -> counts.merge(a.remote().mode(), 1L, Long::sum));
        return counts.entrySet().stream().map(e -> Format.mode(e.getKey()) + " " + e.getValue())
                .collect(Collectors.joining("、"));
    }

    private static String salarySummary(List<Analysis> items) {
        List<Long> monthly = items.stream().flatMap(a -> a.salary().stream())
                .filter(s -> s instanceof Salary.Range r && r.currency().equals("TWD"))
                .map(s -> ((Salary.Range) s).monthlyMin()).sorted().toList();
        if (monthly.isEmpty()) {
            return "無";
        }
        int n = monthly.size();
        long median = n % 2 == 1 ? monthly.get(n / 2) : Math.round((monthly.get(n / 2 - 1) + monthly.get(n / 2)) / 2.0);
        return n + " 筆，中位數 " + Format.money(median) + "，範圍 " + Format.money(monthly.get(0)) + "–"
                + Format.money(monthly.get(n - 1));
    }

    private static String techSummary(List<Analysis> items) {
        Map<String, Long> counts = new LinkedHashMap<>();
        items.forEach(a -> a.tech().forEach(t -> counts.merge(t, 1L, Long::sum)));
        if (counts.isEmpty()) {
            return "無";
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_TECH)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .collect(Collectors.joining("、"));
    }
}
