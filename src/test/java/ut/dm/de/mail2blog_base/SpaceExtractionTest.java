package ut.dm.de.mail2blog_base;

import de.dm.mail2blog.base.ISpaceKeyValidator;
import de.dm.mail2blog.base.Mail2BlogBaseConfiguration;
import de.dm.mail2blog.base.SpaceExtractor;
import de.dm.mail2blog.base.SpaceInfo;
import de.dm.mail2blog.base.SpaceRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpaceExtractionTest {
    SpaceExtractor spaceExtractor;

    @Before
    public void setUp() throws Exception
    {
        // Create spaceKeyExtractor.
        spaceExtractor = new SpaceExtractor(new ISpaceKeyValidator() {
            public boolean spaceExists(String spaceKey) {
                return true;
            }
        });
    }

    /**
     * Test that the default space is found.
     */
    @Test
    public void testDefaultSpace() {
        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
        .defaultSpace("DefaultSpace")
        .build();

        Message message = mock(Message.class);
        List<SpaceInfo> spaces = spaceExtractor.getSpaces(mail2BlogBaseConfiguration, message);

        assertEquals("Expected to find one space (the default space)", 1, spaces.size());
        assertEquals("DefaultSpace", spaces.get(0).getSpaceKey());
    }

    /**
     * Test space rules.
     */
    @Test
    public void testSpaceRules() throws Exception {

        String[][] table = new String[][]{
            //            Field,     Operator,   Value,                  Action,     Space
            new String[]{ "from",    "is",       "alice@example.org",    "copy",     "marseille", },
            new String[]{ "to",      "contains", "shop",                 "move",     "paris",     },
            new String[]{ "cc",      "start",    "alice@",               "copy",     "grenoble",  },
            new String[]{ "to/cc",   "end",      "@example.org",         "copy",     "lyon",      },
            new String[]{ "subject", "regexp",   "[0-9]+",               "copy",     "bordeaux",  },
        };

        Object[][] messages = new Object[][]{
            //            From,                TO,                CC,                  Subject,    Spaces
            new Object[]{ "alice@example.org", "info@shop.de",    "alice@example.com", "test123",  new String[]{ "marseille", "paris" } },
            new Object[]{ "alice@example.org", "bob@example.org", "alice@example.org", "test123",  new String[]{ "marseille", "grenoble", "lyon", "bordeaux", "defaultSpace" } },
        };

        SpaceRule[] spaceRules = new SpaceRule[table.length];
        for (int i = 0; i < table.length; i++) {
            spaceRules[i] = SpaceRule.builder()
                .field(table[i][0])
                .operator(table[i][1])
                .value(table[i][2])
                .action(table[i][3])
                .space(table[i][4])
                .build();
        }

        Mail2BlogBaseConfiguration mail2BlogBaseConfiguration = Mail2BlogBaseConfiguration.builder()
            .spaceRules(spaceRules)
            .defaultSpace("defaultSpace")
        .build();

        for (Object[] m : messages) {
            String from        = (String)m[0];
            String to          = (String)m[1];
            String cc          = (String)m[2];
            String subject     = (String)m[3];
            String[] spaceKeys = (String[])m[4];

            String strMessage = "from:" + from + " to:" + to + " cc:" +cc + " subject:" + subject;

            // Create message
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(new Address[] { new InternetAddress(from) });
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[] { new InternetAddress(to) });
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[] { new InternetAddress(cc) });
            when(message.getSubject()).thenReturn(subject);

            // Try to get spaces
            List<SpaceInfo> spaceInfos = spaceExtractor.getSpaces(mail2BlogBaseConfiguration, message);

            // Check that the right number of spaces where created.
            assertEquals("Wrong number of spaces found for message " + strMessage, spaceKeys.length, spaceInfos.size());

            int i = 0;
            for (String spaceKey: spaceKeys) {
                assertEquals("Wrong space keys for message " + strMessage, spaceKey, spaceInfos.get(i).getSpaceKey());
                i++;
            }
        }

        Address[] recipientsTo = new Address[] {
            new InternetAddress("alice+london@example.org"),
            new InternetAddress("bob+paris@example.org"),
        };
        Message message = mock(Message.class);
    }
}
