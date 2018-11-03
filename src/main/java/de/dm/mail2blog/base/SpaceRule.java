package de.dm.mail2blog.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A rule can be used to map e-mails to spaces.
 * To allow for easy serialization all values are stored as String.
 */
@Data
@Builder
@JsonDeserialize(builder = SpaceRule.SpaceRuleBuilder.class)
public class SpaceRule {
    private String field;       // SpaceRuleFields
    private String operator;    // SpaceRuleOperators
    private String value;       // Value to match against.
    private String action;      // SpaceRuleActions
    private String space;       // SpaceRuleSpaces
    private String contentType = ContentTypes.BlogPost; // ContentType

    /**
     * Check that all fields in space rule are valid.
     */
    public void validate(ISpaceKeyValidator spaceKeyValidator) throws SpaceRuleValidationException {
        if (!SpaceRuleFields.validate(field)) {
            throw new SpaceRuleValidationException("invalid field '" + field + "'");
        }

        if (!SpaceRuleOperators.validate(operator)) {
            throw new SpaceRuleValidationException("invalid operator '" + operator + "'");
        }

        if (!SpaceRuleActions.validate(action)) {
            throw new SpaceRuleValidationException("invalid action '" + action + "'");
        }

        if (!SpaceRuleSpaces.validate(operator, spaceKeyValidator, space)) {
            throw new SpaceRuleValidationException("invalid space key '" + space + "'");
        }

        if (!ContentTypes.validate(contentType)) {
            throw new SpaceRuleValidationException("invalid content type '" + contentType + "'");
        }

        // Check that regexp compiles
        if (operator.equals(SpaceRuleOperators.Regexp)) {
            try{
                Pattern.compile(value);
            }catch(PatternSyntaxException e) {
                throw new SpaceRuleValidationException("invalid regexp", e);
            }
        }
    }

    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpaceRuleBuilder { }
}
