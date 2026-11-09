package io.github.yinyaoqing.jobtext.salary;

import io.github.yinyaoqing.jobtext.salary.Salary.Period;
import io.github.yinyaoqing.jobtext.salary.Salary.Range;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the first salary statement in free text. Understands Taiwanese job-board formats
 * ("月薪65,000~125,000元", "年薪80~120萬", "時薪200元", "待遇面議（經常性薪資達4萬元或以上）")
 * and common English ones ("US$120k–150k / year", "€60,000 per year", "$45/hr").
 */
public final class SalaryParser {

    private static final String NUM = "\\d[\\d,]*(?:\\.\\d+)?";
    private static final String DASH = "(?:~|～|-|–|—|至|到)";

    private static final Pattern ZH_RANGE = Pattern.compile(
            "(?<period>月薪|年薪|時薪|日薪)\\s*[:：]?\\s*(?:NT\\$|新台幣|新臺幣)?\\s*"
                    + "(?<min>" + NUM + ")\\s*(?<minUnit>萬|千)?\\s*元?"
                    + "(?:\\s*" + DASH + "\\s*(?<max>" + NUM + ")\\s*(?<maxUnit>萬|千)?\\s*元?)?"
                    + "\\s*(?<open>以上)?");

    private static final Pattern NEGOTIABLE = Pattern.compile("面議");

    private static final Pattern ZH_FLOOR = Pattern.compile(
            "(?:經常性薪資|月薪)\\s*(?:達|為)?\\s*(?<min>" + NUM + ")\\s*(?<unit>萬|千)?\\s*元?\\s*或?以上");

    private static final String CURRENCY = "(?:US\\$|USD|NT\\$|TWD|€|EUR|£|GBP|\\$)";

    private static final Pattern EN_RANGE = Pattern.compile(
            "(?i)(?<cur>" + CURRENCY + ")\\s*(?<min>" + NUM + ")(?<minK>[km](?![a-z]))?"
                    + "(?:\\s*(?:-|–|—|to)\\s*" + CURRENCY + "?\\s*(?<max>" + NUM + ")(?<maxK>[km](?![a-z]))?)?"
                    + "(?:\\s*(?:/|per|a)\\s*(?<per>year|yr|annum|month|mo|hour|hr|day))?");

    /** Longest digit string accepted, so a pasted ID number never overflows a long. */
    private static final int MAX_DIGITS = 15;

    private SalaryParser() {
    }

    public static Optional<Salary> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (NEGOTIABLE.matcher(text).find()) {
            Matcher floor = ZH_FLOOR.matcher(text);
            while (floor.find()) {
                Optional<Range> range = range(Period.MONTH, floor.group("min"), floor.group("unit"), null, null, "TWD", true);
                if (range.isPresent()) {
                    return Optional.of(new Salary.Negotiable(range));
                }
            }
            return Optional.of(new Salary.Negotiable(Optional.empty()));
        }
        Matcher zh = ZH_RANGE.matcher(text);
        while (zh.find()) {
            String maxUnit = zh.group("maxUnit");
            String minUnit = zh.group("minUnit") != null ? zh.group("minUnit") : maxUnit; // 年薪80~120萬
            Optional<Range> range = range(zhPeriod(zh.group("period")), zh.group("min"), minUnit,
                    zh.group("max"), maxUnit, "TWD", zh.group("open") != null);
            if (range.isPresent()) {
                return Optional.of(range.get());
            }
        }
        Matcher en = EN_RANGE.matcher(text);
        while (en.find()) {
            String maxK = en.group("maxK");
            String minK = en.group("minK") != null ? en.group("minK") : maxK; // $120-150k
            Optional<Range> range = range(null, en.group("min"), minK, en.group("max"), maxK,
                    currency(en.group("cur")), false, en.group("per"));
            if (range.isPresent()) {
                return Optional.of(range.get());
            }
        }
        return Optional.empty();
    }

    private static Optional<Range> range(Period period, String min, String minUnit, String max, String maxUnit,
            String currency, boolean openEnded) {
        return range(period, min, minUnit, max, maxUnit, currency, openEnded, null);
    }

    private static Optional<Range> range(Period period, String min, String minUnit, String max, String maxUnit,
            String currency, boolean openEnded, String englishPeriod) {
        try {
            long lo = amount(min, minUnit);
            long hi = max == null ? lo : amount(max, maxUnit);
            Period p = period != null ? period : englishPeriod(englishPeriod, lo);
            return Optional.of(new Range(p, lo, hi, currency, openEnded));
        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty(); // unusable figure: try the next match
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // min greater than max
        }
    }

    static long amount(String raw, String unit) {
        String digits = raw.replace(",", "");
        if (digits.length() > MAX_DIGITS) {
            throw new NumberFormatException("too many digits: " + raw);
        }
        long multiplier = switch (unit == null ? "" : unit.toLowerCase(Locale.ROOT)) {
            case "m" -> 1_000_000;
            case "萬" -> 10_000;
            case "千", "k" -> 1_000;
            default -> 1;
        };
        return new BigDecimal(digits).multiply(BigDecimal.valueOf(multiplier))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static Period zhPeriod(String word) {
        return switch (word) {
            case "年薪" -> Period.YEAR;
            case "時薪" -> Period.HOUR;
            case "日薪" -> Period.DAY;
            default -> Period.MONTH;
        };
    }

    /** With no period written, small figures are hourly and the rest yearly. */
    private static Period englishPeriod(String word, long amount) {
        if (word == null) {
            return amount < 500 ? Period.HOUR : Period.YEAR;
        }
        return switch (word.toLowerCase(Locale.ROOT)) {
            case "month", "mo" -> Period.MONTH;
            case "hour", "hr" -> Period.HOUR;
            case "day" -> Period.DAY;
            default -> Period.YEAR;
        };
    }

    private static String currency(String symbol) {
        return switch (symbol.toUpperCase(Locale.ROOT)) {
            case "NT$", "TWD" -> "TWD";
            case "€", "EUR" -> "EUR";
            case "£", "GBP" -> "GBP";
            default -> "USD";
        };
    }
}
