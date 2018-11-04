package de.dm.mail2blog.base;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * Get a html filter according to the mail configuration.
 */
public abstract class HtmlFilterFactory {

    /**
     * Build one htmlPolicyFactory from htmlFilters to filter HTML.
     *
     * @return policy factory one can use to filter html
     */
    public static PolicyFactory makeHtmlFilter(Mail2BlogBaseConfiguration mail2BlogBaseConfiguration) {
        PolicyFactory result = new HtmlPolicyBuilder().toFactory();
        if (mail2BlogBaseConfiguration.getHtmlFilterFormatting()) { result = result.and(HtmlSanitizers.FORMATTING); }
        if (mail2BlogBaseConfiguration.getHtmlFilterBlocks()) { result = result.and(HtmlSanitizers.BLOCKS); }
        if (mail2BlogBaseConfiguration.getHtmlFilterImages()) { result = result.and(HtmlSanitizers.IMAGES); }
        if (mail2BlogBaseConfiguration.getHtmlFilterLinks()) { result = result.and(HtmlSanitizers.LINKS); }
        if (mail2BlogBaseConfiguration.getHtmlFilterStyles()) { result = result.and(HtmlSanitizers.STYLES); }
        if (mail2BlogBaseConfiguration.getHtmlFilterTables()) { result = result.and(HtmlSanitizers.TABLES); }
        return result;
    }
}
