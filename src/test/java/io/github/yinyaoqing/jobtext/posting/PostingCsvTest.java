package io.github.yinyaoqing.jobtext.posting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yinyaoqing.jobtext.csv.CsvFormatException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class PostingCsvTest {

    private static PostingCsv.LoadResult load(String csv) throws IOException {
        return PostingCsv.read(new StringReader(csv));
    }

    @Test
    void readsRequiredAndOptionalColumnsInAnyOrder() throws IOException {
        PostingCsv.LoadResult r = load("""
                Description,ID,Title,Company,Salary
                "<p>React</p>",p1,  Frontend   Engineer ,Acme,月薪60000元
                """);
        assertEquals(List.of(), r.errors());
        Posting p = r.postings().get(0);
        assertEquals("p1", p.id());
        assertEquals("Frontend Engineer", p.title());
        assertEquals("月薪60000元", p.salary());
        assertEquals("", p.location());
    }

    @Test
    void skipsBadRowsWithLineNumbers() throws IOException {
        PostingCsv.LoadResult r = load("""
                id,title,company,description
                ok,T,C,D
                short,T,C
                ,T,C,D
                bad id!,T,C,D
                ok,T2,C2,D2
                x,,C,D
                """);
        assertEquals(1, r.postings().size());
        assertEquals(List.of(
                new RowError(3, "expected 4 fields, found 3"),
                new RowError(4, "id is empty"),
                new RowError(5, "id 'bad id!' may only use letters, digits, '.', '_' and '-' (max 64)"),
                new RowError(6, "duplicate id 'ok' (first seen on line 2)"),
                new RowError(7, "title is empty")), r.errors());
    }

    @Test
    void rejectsOversizedFields() throws IOException {
        String big = "x".repeat(PostingCsv.MAX_FIELD_CHARS + 1);
        PostingCsv.LoadResult r = load("id,title,company,description\np,T,C," + big + "\n");
        assertTrue(r.errors().get(0).message().startsWith("field 'description' is longer than"));
    }

    @Test
    void cleansHiddenCharacters() throws IOException {
        PostingCsv.LoadResult r = load("id,title,company,description\np,\"Eng‮ineer\",C,\"a\u0000b\"\n");
        assertEquals("Engineer", r.postings().get(0).title());
        assertEquals("ab", r.postings().get(0).description());
    }

    @Test
    void ignoresBlankLines() throws IOException {
        assertEquals(1, load("\nid,title,company,description\n\np,T,C,D\n\n").postings().size());
    }

    @Test
    void headerProblemsStopTheRun() {
        CsvFormatException missing = assertThrows(CsvFormatException.class, () -> load("id,title\n1,x\n"));
        assertTrue(missing.getMessage().contains("company, description"));
        assertThrows(CsvFormatException.class, () -> load("id,id,title,company,description\n"));
        assertThrows(CsvFormatException.class, () -> load(""));
    }
}
