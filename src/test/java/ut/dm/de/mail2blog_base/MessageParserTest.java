package ut.dm.de.mail2blog_base;

import de.dm.mail2blog.base.FileTypeBucket;
import de.dm.mail2blog.base.Mail2BlogBaseConfiguration;
import de.dm.mail2blog.base.MailPartData;
import de.dm.mail2blog.base.MessageParser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageParserTest {
    /**
     * Example mail message from resources/exampleMail.eml.
     */
    static Message exampleMessage;

    private Mail2BlogBaseConfiguration.Mail2BlogBaseConfigurationBuilder mail2BlogBaseConfigurationBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Read in example mail from disk.
        InputStream is = MessageParserTest.class.getClassLoader().getResourceAsStream("exampleMail.eml");
        exampleMessage = new MimeMessage(null, is);
    }

    /**
     * Test that the charset is correctly returned from given header.
     */
    @Test
    public void testGetCharsetFromHeader() {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder().build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        Charset utf8 = messageParser.getCharsetFromHeader("text/plain; charset=\"utf-8\"");
        Charset iso88591 = messageParser.getCharsetFromHeader("text/html; charset=\"ISO-8859-1\"");
        Charset cp437 = messageParser.getCharsetFromHeader("text/plain; charset=CP437");
        Charset missing = messageParser.getCharsetFromHeader("text/plain");
        Charset invalid = messageParser.getCharsetFromHeader("text/plain; charset=xxx123");

        assertEquals(Charset.forName("utf-8"), utf8);
        assertEquals(Charset.forName("ISO-8859-1"), iso88591);
        assertEquals(Charset.forName("CP437"), cp437);
        assertEquals("Expected default charset, when charset is missing from content-type.", Charset.defaultCharset(), missing);
        assertEquals("Expected default charset, when an invalid/unknown charset is given in content-type.", Charset.defaultCharset(), invalid);
    }

    /**
     * Check that the content of the example mail is properly extracted.
     * When using the default values.
     */
    @Test
    public void testExtractionWithDefaults() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder().build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        assertEquals("Expected two parts in mail", 2, content.size());

        // Get html and attachment.
        MailPartData htmlPart = content.get(0);
        MailPartData attachment = content.get(1);

        assertEquals("Wrong mimeType for html part", "text/html", htmlPart.getContentType());
        assertTrue("Could not find <p>Lieber Bob,</p> in html", htmlPart.getHtml().contains("<p>Lieber Bob,</p>"));

        assertEquals("Wrong content-id for attachment", "<6FA75120-9E1A-45DE-9001-620110B831AD>", attachment.getContentID());
        assertEquals("Wrong mimeType for attachment", "image/gif", attachment.getContentType());
        assertEquals("Wrong filename for attachement", "dm-logo.gif", attachment.getAttachementData().getFilename());
        assertEquals("Wrong media type for attachement", "image/gif", attachment.getAttachementData().getMediaType());
        assertEquals("Wrong file size for attachement", 2155, attachment.getAttachementData().getFileSize());
    }

    /**
     * Check that text is extracted instead of html if the option is used.
     */
    @Test
    public void testExtractionWithPreferredText() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
                .preferredContentTypes(new String[]{"text/plain", "text/html"})
                .build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        assertEquals("Expected two parts in mail", 2, content.size());

        // Get text part.
        MailPartData textPart = content.get(0);

        assertEquals("Wrong mimeType for text part", "text/plain", textPart.getContentType());
        assertTrue("Could not find - ALANA in text", textPart.getHtml().contains("- ALANA"));
    }

    /**
     * Check that no attachment is added if gif image/gif is not in allowed file types.
     */
    @Test
    public void testExtractionWithForbiddenMimeType() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
                .fileTypeBucket(FileTypeBucket.fromString("jpg image/jpg"))
                .build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. But found one.", part.getAttachementData());
        }

    }

    /**
     * Check that no attachment is added if the max number of allowed attachments is 0.
     */
    @Test
    public void testExtractionWithMaxNumberOfAllowedAttachments() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
                .maxAllowedNumberOfAttachments(0)
                .build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. Buf found one.", part.getAttachementData());
        }

    }

    /**
     * Check that no attachment is added if the maximum size for an attachment is 0 mb.
     */
    @Test
    public void testExtractionWithMaxAttachmentSize() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
            .maxAllowedAttachmentSizeInBytes(0)
            .build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);

        // Extract data from message.
        List<MailPartData> content = messageParser.getContent();

        for (MailPartData part : content) {
            assertNull("Expected no attachment. But found one.", part.getAttachementData());
        }
    }

    /**
     * Test getting the user from a mail message.
     */
    @Test
    public void testGetUser() throws Exception {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder().build();

        MessageParser messageParser = new MessageParser(exampleMessage, mail2BlogBaseConfiguration);
        assertEquals("Failed to get user", "alice@example.org", messageParser.getSenderEmail());
    }
}

