package io.github.yinyaoqing.jobtext.report;

import io.github.yinyaoqing.jobtext.salary.Salary;
import io.github.yinyaoqing.jobtext.text.RemotePolicy;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/** Display strings shared by the Markdown and CSV reports. */
final class Format {

    private Format() {
    }

    static String money(long amount) {
        return String.format(Locale.ROOT, "%,d", amount);
    }

    static String salary(Optional<Salary> salary) {
        if (salary.isEmpty()) {
            return "";
        }
        return switch (salary.get()) {
            case Salary.Negotiable n -> n.floor().map(f -> "面議（" + range(f) + "）").orElse("面議");
            case Salary.Range r -> range(r);
        };
    }

    private static String range(Salary.Range r) {
        String amount;
        if (r.currency().equals("TWD")) {
            amount = money(r.monthlyMin()) + (r.max() > r.min() ? "–" + money(r.monthlyMax()) : "")
                    + (r.openEnded() ? " 以上" : "") + " /月";
        } else {
            amount = r.currency() + " " + money(r.min()) + (r.max() > r.min() ? "–" + money(r.max()) : "")
                    + (r.openEnded() ? "+" : "") + " /" + period(r.period());
        }
        return amount;
    }

    private static String period(Salary.Period p) {
        return switch (p) {
            case HOUR -> "時";
            case DAY -> "日";
            case MONTH -> "月";
            case YEAR -> "年";
        };
    }

    static String mode(RemotePolicy.Mode mode) {
        return switch (mode) {
            case FULL_REMOTE -> "完全遠端";
            case HYBRID -> "部分遠端";
            case ONSITE -> "到辦公室";
            case UNKNOWN -> "未註明";
        };
    }

    static String offsets(List<ZoneOffset> offsets) {
        return offsets.stream().map(o -> o.equals(ZoneOffset.UTC) ? "UTC" : "UTC" + o.getId())
                .collect(Collectors.joining(", "));
    }
}
