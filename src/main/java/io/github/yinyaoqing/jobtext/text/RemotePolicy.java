package io.github.yinyaoqing.jobtext.text;

import java.util.Objects;
import java.util.OptionalInt;

/** Remote-work arrangement stated in a posting. */
public record RemotePolicy(Mode mode, OptionalInt officeDaysPerWeek) {

    public enum Mode { FULL_REMOTE, HYBRID, ONSITE, UNKNOWN }

    public RemotePolicy {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(officeDaysPerWeek, "officeDaysPerWeek");
    }
}
