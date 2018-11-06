package ut.de.dm.mail2blog.base;

import de.dm.mail2blog.base.HtmlFilterFactory;
import de.dm.mail2blog.base.Mail2BlogBaseConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.owasp.html.PolicyFactory;

import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/**
 * Test the html filter. Just some basics tests to make sure the configuration flags are properly interpreted.
 * Complex anti XSS test are done by the OWASP html sanitizer library.
 */
@RunWith(MockitoJUnitRunner.class)
public class HtmlFilterTest {
    private static String HTML_SAMPLE =
        "<h1 style='background-color: #000;'>Hello >World</h1>"
        + "<img src=\"http://example.org\"/>"
        + " <p>This is a<br> test.</p>"
        + " <strong><a onmouseover=evil() href='http://example.org'>Click Me</a></strong>"
        + "<table><tr><td>"
        + "<script>alert('XSS');</script>"
        + "</td></tr></table>";

    private static String COMPLEX_HTML_TABLE_SAMPLE = new Scanner(
        HtmlFilterTest.class.getClassLoader().getResourceAsStream("sample-table.html"),
        "UTF-8"
    ).useDelimiter("\\A").next();

    @Test
    public void testDisallowAllHtml() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(false)
            .htmlFilterImages(false)
            .htmlFilterLinks(false)
            .htmlFilterStyles(false)
            .htmlFilterTables(false)
        .build());

        assertEquals("Hello &gt;World This is a test. Click Me", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowFormatting() throws Exception {
         PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(true)
            .htmlFilterBlocks(false)
            .htmlFilterImages(false)
            .htmlFilterLinks(false)
            .htmlFilterStyles(false)
            .htmlFilterTables(false)
        .build());

        assertEquals("Hello &gt;World This is a<br /> test. <strong>Click Me</strong>", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowBlocks() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(true)
            .htmlFilterImages(false)
            .htmlFilterLinks(false)
            .htmlFilterStyles(false)
            .htmlFilterTables(false)
        .build());

        assertEquals("<h1>Hello &gt;World</h1> <p>This is a test.</p> Click Me", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowImages() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(false)
            .htmlFilterImages(true)
            .htmlFilterLinks(false)
            .htmlFilterStyles(false)
            .htmlFilterTables(false)
        .build());

        assertEquals("Hello &gt;World<img src=\"http://example.org\" /> This is a test. Click Me", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowLinks() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(false)
            .htmlFilterImages(false)
            .htmlFilterLinks(true)
            .htmlFilterStyles(false)
            .htmlFilterTables(false)
        .build());

        assertEquals("Hello &gt;World This is a test. <a href=\"http://example.org\" rel=\"nofollow\">Click Me</a>", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowBlocksAndStyles() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(true)
            .htmlFilterImages(false)
            .htmlFilterLinks(false)
            .htmlFilterStyles(true)
            .htmlFilterTables(false)
        .build());

        assertEquals("<h1 style=\"background-color: #000;\">Hello &gt;World</h1> <p>This is a test.</p> Click Me", filter.sanitize(HTML_SAMPLE));
    }

    @Test
    public void testAllowTableAndStyles() throws Exception {
        PolicyFactory filter = HtmlFilterFactory.makeHtmlFilter(Mail2BlogBaseConfiguration.builder()
            .htmlFilterFormatting(false)
            .htmlFilterBlocks(false)
            .htmlFilterImages(false)
            .htmlFilterLinks(false)
            .htmlFilterStyles(true)
            .htmlFilterTables(true)
        .build());

        assertEquals("Hello &gt;World This is a test. Click Me<table><tr><td></td></tr></table>", filter.sanitize(HTML_SAMPLE));
        assertEquals(COMPLEX_HTML_TABLE_SAMPLE, filter.sanitize(COMPLEX_HTML_TABLE_SAMPLE));
    }
}
