package de.dm.mail2blog.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class SpaceExtractor {
    @NonNull ISpaceKeyValidator spaceKeyValidator;

    /**
     * @param mail2BlogBaseConfiguration The config to use
     * @param message The mail message from which to extract the space key.
     *
     * @return Returns a list of space keys
     */
    public List<SpaceInfo> getSpaces(Mail2BlogBaseConfiguration mail2BlogBaseConfiguration, Message message)
    {
        // Evaluate space rules.
        ArrayList<SpaceInfo> spaces = new ArrayList<SpaceInfo>();
        HashMap<String, Void> seenSpaceKeys = new HashMap<String, Void>();
        for (SpaceRule rule : mail2BlogBaseConfiguration.getSpaceRules()) {
            boolean ruleMatched = false;

            try {
                List<String > values = extractValues(rule, message);
                for (String value: values) {
                    if (evalCondition(rule, value)) {
                        ruleMatched = true;

                        String spaceKey = extractSpaceKey(rule, value);

                        if (!seenSpaceKeys.containsKey(spaceKey)) {
                            seenSpaceKeys.put(spaceKey, null);

                            // Get space
                            if (!spaceKeyValidator.spaceExists(spaceKey)) {
                                log.warn("Mai2Blog: invalid space key " + spaceKey);
                                continue;
                            }

                            // Add space.
                            spaces.add(SpaceInfo.builder().spaceKey(spaceKey).contentType(rule.getContentType()).build());
                        }
                    }
                }
            } catch (Exception e) {
                String info = "";
                try {
                    info = new ObjectMapper().writeValueAsString(rule);
                    info = " for SpaceRule" + info;
                } catch (Exception e2) {}

                log.warn("Mail2Blog: (" + e.toString() + ")" + info, e);
            }

            // A move rule is always the finial rule that gets applied.
            if (ruleMatched && SpaceRuleActions.MOVE.equals(rule.getAction())) {
                return spaces;
            }
        }

        if (!spaceKeyValidator.spaceExists(mail2BlogBaseConfiguration.getDefaultSpace())) {
            log.warn("Mail2Blog: Invalid default space");
        } else {
            // Add default space to spaceKeys.
            spaces.add(SpaceInfo.builder()
                .spaceKey(mail2BlogBaseConfiguration.getDefaultSpace())
                .contentType(mail2BlogBaseConfiguration.getDefaultContentType())
                .build()
            );
        }

        return spaces;
    }

    /**
     * Get one ore multiple possible values from the given message according to the field specified in the SpaceRule.
     */
    private List<String> extractValues(SpaceRule rule, Message message) throws MessagingException {
        ArrayList<String> values = new ArrayList<String>();

        if (SpaceRuleFields.FROM.equals(rule.getField())) {
            Address[] from = message.getFrom();
            if (from != null) {
                for (Address a : from) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.TO.equals(rule.getField()) || SpaceRuleFields.ToCC.equals(rule.getField())) {
            Address[] to = message.getRecipients(Message.RecipientType.TO);
            if (to != null) {
                for (Address a : to) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.CC.equals(rule.getField()) || SpaceRuleFields.ToCC.equals(rule.getField())) {
            Address[] cc = message.getRecipients(Message.RecipientType.CC);
            if (cc != null) {
                for (Address a : cc) {
                    String emailAddress = (a instanceof InternetAddress)
                            ? ((InternetAddress) a).getAddress()
                            : a.toString();
                    values.add(emailAddress.trim());
                }
            }
        }

        if (SpaceRuleFields.SUBJECT.equals(rule.getField())) {
            values.add(message.getSubject().trim());
        }

        return values;
    }

    /**
     * Check if given value fulfills the condition given in the SpaceRule.
     *
     * @param rule
     *  The rule to evaluate.
     *
     * @param value
     *  The value extracted from a field.
     *
     * @return
     *  True if the condition is fulfilled, false if not.
     */
    public boolean evalCondition(SpaceRule rule, String value) throws Exception {
        if (SpaceRuleOperators.Is.equals(rule.getOperator())) {
            return StringUtils.equalsIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.Contains.equals(rule.getOperator())) {
            return StringUtils.containsIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.StartsWith.equals(rule.getOperator())) {
            return StringUtils.startsWithIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.EndsWith.equals(rule.getOperator())) {
            return StringUtils.endsWithIgnoreCase(value, rule.getValue());
        } else if (SpaceRuleOperators.Regexp.equals(rule.getOperator())) {
            try {
                TimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
                return timeLimiter.callWithTimeout(() -> {
                    Pattern pattern = Pattern.compile(rule.getValue(), Pattern.CASE_INSENSITIVE);
                    return pattern.matcher(new InterruptibleCharSequence(value)).find();
                }, 100, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new Exception("failed to evaluate regexp in space rules", e);
            }
        }

        return false;
    }

    /**
     * Get the space key for a rule.
     *
     * Usually this is just rule.space, but for regexps the space key can be extracted from value.
     */
    public String extractSpaceKey(SpaceRule rule, String value) throws Exception {
        // Extract space key with regexp.
        if (
            SpaceRuleOperators.Regexp.equals(rule.getOperator()) &&
            (SpaceRuleSpaces.CapturingGroup0.equals(rule.getSpace()) || SpaceRuleSpaces.CapturingGroup1.equals(rule.getSpace()))
        ) {
            try {
                TimeLimiter timeLimiter = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());
                return timeLimiter.callWithTimeout(() -> {
                    Pattern pattern = Pattern.compile(rule.getValue(), Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(new InterruptibleCharSequence(value));

                    if (!matcher.find()) {
                        throw new Exception("regexp did not match");
                    }

                    if (SpaceRuleSpaces.CapturingGroup0.equals(rule.getSpace())) {
                        return matcher.group(0);
                    } else {
                        if (matcher.groupCount() < 1) {
                            throw new Exception("no capturing group 1");
                        }
                        return matcher.group(1);
                    }
                }, 100, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new Exception("failed to extract space key with regexp", e);
            }
        }

        return rule.getSpace();
    }
}
