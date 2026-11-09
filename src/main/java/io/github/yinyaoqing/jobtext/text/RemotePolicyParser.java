package io.github.yinyaoqing.jobtext.text;

import io.github.yinyaoqing.jobtext.text.RemotePolicy.Mode;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Classifies remote-work wording in Chinese and English and reads "N days a week in the office". */
public final class RemotePolicyParser {

    private static final Pattern ONSITE = Pattern.compile(
            "(?i)不可遠端|無法遠端|不提供遠端|無遠端工作|no remote|on-?site only|fully on-?site|in-office only");
    private static final Pattern HYBRID = Pattern.compile("(?i)部分遠端|混合辦公|混合式辦公|\\bhybrid\\b");
    private static final Pattern FULL = Pattern.compile(
            "(?i)完全遠端|全遠端|全職遠端|fully remote|100% remote|remote[- ]first|work from anywhere");

    private static final Pattern ZH_OFFICE_DAYS = Pattern.compile(
            "(?:一週|每週|一周|每周|每星期|一星期|週|周)\\s*(?:需|要)?\\s*(?:進|到|回)(?:公司|辦公室)"
                    + "[^\\n，。,.；;]{0,8}?(?<n>[一二兩三四五1-5])\\s*(?:次|天|日)");
    private static final Pattern EN_OFFICE_DAYS = Pattern.compile(
            "(?i)\\b(?<n>[1-5]|one|two|three|four|five)\\s*(?:days?|x)\\s*(?:a|per|/)\\s*week\\s*(?:in|at)\\s*(?:the\\s*)?office");

    private RemotePolicyParser() {
    }

    public static RemotePolicy parse(String text) {
        if (text == null || text.isBlank()) {
            return new RemotePolicy(Mode.UNKNOWN, OptionalInt.empty());
        }
        OptionalInt days = officeDays(text);
        Mode mode;
        if (ONSITE.matcher(text).find()) {
            mode = Mode.ONSITE;
        } else if (HYBRID.matcher(text).find() || days.isPresent()) {
            mode = Mode.HYBRID;
        } else if (FULL.matcher(text).find()) {
            mode = Mode.FULL_REMOTE;
        } else {
            mode = Mode.UNKNOWN;
        }
        return new RemotePolicy(mode, days);
    }

    private static OptionalInt officeDays(String text) {
        Matcher zh = ZH_OFFICE_DAYS.matcher(text);
        if (zh.find()) {
            return OptionalInt.of(number(zh.group("n")));
        }
        Matcher en = EN_OFFICE_DAYS.matcher(text);
        if (en.find()) {
            return OptionalInt.of(number(en.group("n")));
        }
        return OptionalInt.empty();
    }

    static int number(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "一", "1", "one" -> 1;
            case "二", "兩", "2", "two" -> 2;
            case "三", "3", "three" -> 3;
            case "四", "4", "four" -> 4;
            case "五", "5", "five" -> 5;
            default -> throw new IllegalArgumentException("not a day count: " + token);
        };
    }
}
