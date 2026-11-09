package io.github.yinyaoqing.jobtext.posting;

import io.github.yinyaoqing.jobtext.salary.Salary;
import io.github.yinyaoqing.jobtext.salary.SalaryParser;
import io.github.yinyaoqing.jobtext.text.HtmlText;
import io.github.yinyaoqing.jobtext.text.RemotePolicy;
import io.github.yinyaoqing.jobtext.text.RemotePolicyParser;
import io.github.yinyaoqing.jobtext.text.TechStack;
import io.github.yinyaoqing.jobtext.text.TimeZoneParser;
import java.util.List;
import java.util.Optional;

/** Everything extracted from one posting. */
public record Analysis(Posting posting, String plainDescription, Optional<Salary> salary, RemotePolicy remote,
        TimeZoneParser.Mentions timeZones, List<String> tech) {

    public Analysis {
        tech = List.copyOf(tech);
    }

    /** Dedicated columns win over the description; the description fills the gaps. */
    public static Analysis of(Posting p) {
        String plain = HtmlText.toPlainText(p.description());
        Optional<Salary> salary = SalaryParser.parse(p.salary()).or(() -> SalaryParser.parse(plain));
        RemotePolicy remote = RemotePolicyParser.parse(p.remote() + "\n" + plain);
        TimeZoneParser.Mentions zones = TimeZoneParser.parse(p.location() + "\n" + plain);
        List<String> tech = TechStack.detect(p.title() + "\n" + plain);
        return new Analysis(p, plain, salary, remote, zones, tech);
    }
}
