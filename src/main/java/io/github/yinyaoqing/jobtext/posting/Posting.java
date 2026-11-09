package io.github.yinyaoqing.jobtext.posting;

import java.util.Objects;

/** One cleaned CSV row. Optional columns that are absent are empty strings. */
public record Posting(String id, String title, String company, String location, String salary, String remote,
        String description) {

    public Posting {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(company, "company");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(salary, "salary");
        Objects.requireNonNull(remote, "remote");
        Objects.requireNonNull(description, "description");
    }
}
