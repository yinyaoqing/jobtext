package io.github.yinyaoqing.jobtext.salary;

import java.util.Objects;
import java.util.Optional;

/** A salary found in a posting: either a stated range or "negotiable" (待遇面議). */
public sealed interface Salary permits Salary.Range, Salary.Negotiable {

    enum Period { HOUR, DAY, MONTH, YEAR }

    /**
     * A stated pay range in whole currency units. A single figure has {@code min == max};
     * {@code openEnded} marks "以上" / "or more".
     */
    record Range(Period period, long min, long max, String currency, boolean openEnded) implements Salary {

        /** Working hours and days used to compare hourly, daily and monthly pay. */
        public static final int HOURS_PER_MONTH = 176;
        public static final int DAYS_PER_MONTH = 22;

        public Range {
            Objects.requireNonNull(period, "period");
            Objects.requireNonNull(currency, "currency");
            if (min < 0 || max < min) {
                throw new IllegalArgumentException("invalid salary range " + min + ".." + max);
            }
        }

        public long monthlyMin() {
            return toMonthly(min);
        }

        public long monthlyMax() {
            return toMonthly(max);
        }

        private long toMonthly(long amount) {
            return switch (period) {
                case HOUR -> Math.multiplyExact(amount, HOURS_PER_MONTH);
                case DAY -> Math.multiplyExact(amount, DAYS_PER_MONTH);
                case MONTH -> amount;
                case YEAR -> Math.round(amount / 12.0);
            };
        }
    }

    /** 待遇面議, optionally with the legal monthly floor 104 requires, e.g. "經常性薪資達4萬元以上". */
    record Negotiable(Optional<Range> floor) implements Salary {

        public Negotiable {
            Objects.requireNonNull(floor, "floor");
        }
    }
}
