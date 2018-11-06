package ut.de.dm.mail2blog.base;

import de.dm.mail2blog.base.ISpaceKeyValidator;
import de.dm.mail2blog.base.SpaceRule;
import de.dm.mail2blog.base.SpaceRuleSpaces;
import de.dm.mail2blog.base.SpaceRuleValidationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class SpaceRuleTest {
    public static String VALID_SPACE_KEY = "space42";

    public ISpaceKeyValidator spaceKeyValidator = new ISpaceKeyValidator() {
        public boolean spaceExists(String spaceKey) {
            if (spaceKey.equals(VALID_SPACE_KEY)) {
                return true;
            } else {
                return false;
            }
        }
    };


    /**
     * Test the validation process for space rules.
     */
    @Test
    public void testValidateSpaceRules() throws Exception {
        assertSpaceRule("from", "is", "alpha", "copy", VALID_SPACE_KEY, "blog", true);
        assertSpaceRule("to", "contains", "bravo", "move", VALID_SPACE_KEY, "blog", true);
        assertSpaceRule("cc", "start", "charlie", "copy", VALID_SPACE_KEY, "blog", true);
        assertSpaceRule("subject", "end", "delta", "copy", VALID_SPACE_KEY, "blog", true);

        assertSpaceRule("from", "regexp", "^echo", "copy", VALID_SPACE_KEY, "blog", true);
        assertSpaceRule("from", "regexp", "echo$", "copy", SpaceRuleSpaces.CapturingGroup0, "page", true);
        assertSpaceRule("from", "regexp", "echo ([0-9]*)", "copy", SpaceRuleSpaces.CapturingGroup1, "page", true);

        assertSpaceRule("bogus", "is", "alpha", "copy", VALID_SPACE_KEY, "blog", false); // Invalid field
        assertSpaceRule("from", "nonsense", "alpha", "copy", VALID_SPACE_KEY, "blog",false); // Invalid operator
        assertSpaceRule("from", "is", "alpha", "notworking", VALID_SPACE_KEY, "blog", false); // Invalid action
        assertSpaceRule("from", "is", "alpha", "copy", "nirvana", "blog", false); // Invalid space
        assertSpaceRule("from", "is", "alpha", "copy", SpaceRuleSpaces.CapturingGroup0, "blog",false); // Capturing group not on regexp
        assertSpaceRule("cc", "start", "charlie", "move", SpaceRuleSpaces.CapturingGroup1, "blog",false); // Capturing group not on regexp
        assertSpaceRule("from", "regexp", "^(unclosed group", "copy", VALID_SPACE_KEY, "blog",false); // Invalid regexp
        assertSpaceRule("from", "is", "alpha", "copy", VALID_SPACE_KEY, "bogus", false); // Invalid contentType
    }

    /**
     * Check that space rules are validated properly.
     */
    private void assertSpaceRule(String field, String operator, String value, String action, String space, String contentType, boolean expectedResult) throws Exception {
        SpaceRule spaceRule = SpaceRule.builder()
            .field(field)
            .operator(operator)
            .value(value)
            .action(action)
            .space(space)
            .contentType(contentType)
        .build();

        // Json representation of spaceRule for pretty printing.
        String textSpaceRule = "{"
            +  "field:" + field
            + " operator:" + operator
            + " value:" + value
            + " action:" + action
            + " space:" + space
            + " contentType:" + contentType
            + "}";

        boolean validationResult = false;
        boolean validationException = false;
        try {
            spaceRule.validate(spaceKeyValidator);
            validationResult = true;
        } catch (SpaceRuleValidationException e) {
            validationException = true;
        }

        if (expectedResult) {
            assertTrue(
                "Unexpected exception for SpaceRule" + textSpaceRule,
                validationResult
            );
        } else {
            assertFalse(
                "Expected an exception for SpaceRule" + textSpaceRule,
                validationResult
            );

            assertTrue(
                "Expected an validation exception but got a different one for SpaceRule" + textSpaceRule,
                validationException
            );
        }
    }

}
