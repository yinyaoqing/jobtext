package io.github.yinyaoqing.jobtext.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HtmlTextTest {

    @Test
    void keepsParagraphsAndListItems() {
        String html = "<h2>About</h2><p>We build&nbsp;tools.</p><ul><li>React</li><li>Java &amp; regex</li></ul>";
        assertEquals("About\nWe build tools.\n\n- React\n- Java & regex", HtmlText.toPlainText(html));
    }

    @Test
    void decodesDoubleEscapedAtsContent() {
        String escaped = "&lt;p&gt;Remote &amp;amp; hybrid&lt;/p&gt;&lt;p&gt;UTC+8&lt;/p&gt;";
        assertEquals("Remote & hybrid\nUTC+8", HtmlText.toPlainText(escaped));
    }

    @Test
    void escapedMarkupInTextStaysVisible() {
        assertEquals("Use <b> for bold", HtmlText.toPlainText("<p>Use &lt;b&gt; for bold</p>"));
    }

    @Test
    void dropsScriptsStylesAndComments() {
        String html = "<style>p{color:red}</style><p>Hi<!-- secret --></p><SCRIPT>alert(1)</SCRIPT>";
        assertEquals("Hi", HtmlText.toPlainText(html));
    }

    @Test
    void decodesNumericReferences() {
        assertEquals("€ 中 😀", HtmlText.unescape("&#8364; &#x4E2D; &#x1F600;"));
    }

    @Test
    void invalidNumericReferencesBecomeReplacementCharacter() {
        assertEquals("���", HtmlText.unescape("&#0;&#xD800;&#x110000;"));
    }

    @Test
    void unknownNamedReferencesAreLeftAlone() {
        assertEquals("&bogus; AT&T", HtmlText.unescape("&bogus; AT&T"));
    }

    @Test
    void collapsesWhitespaceIncludingFullWidthSpace() {
        assertEquals("a b\n\nc", HtmlText.toPlainText("a　\t b<br><br><br><br>c"));
    }

    @Test
    void blankInputGivesEmptyString() {
        assertEquals("", HtmlText.toPlainText(null));
        assertEquals("", HtmlText.toPlainText("   "));
    }
}
