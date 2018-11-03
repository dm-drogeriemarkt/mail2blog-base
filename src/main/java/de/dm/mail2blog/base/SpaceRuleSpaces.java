package de.dm.mail2blog.base;

// Spaces that can be used in space rules.
public abstract class SpaceRuleSpaces {
    public static final String CapturingGroup0 = "_group_0";
    public static final String CapturingGroup1 = "_group_1";

    /**
     * Validate that a given string is a valid space.
     */
    public static boolean validate(String operator, ISpaceKeyValidator spaceKeyValidator, String check) {
        if (operator.equals(SpaceRuleOperators.Regexp)) {
            if (SpaceRuleSpaces.CapturingGroup0.equals(check)) return true;
            if (SpaceRuleSpaces.CapturingGroup1.equals(check)) return true;
        }

        if (spaceKeyValidator.spaceExists(check)) {
            return true;
        }
        return false;
    }
}
