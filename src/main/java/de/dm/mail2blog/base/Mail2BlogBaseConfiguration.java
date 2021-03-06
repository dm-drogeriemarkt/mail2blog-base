package de.dm.mail2blog.base;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class Mail2BlogBaseConfiguration {
    @Getter @NonNull private String defaultSpace;
    @Getter @NonNull private SpaceRule[] spaceRules;
    @Getter @NonNull private String defaultContentType;

    // List of preferred content types to use.
    // There are preferred in the order of the list.
    @Getter @NonNull private String[] preferredContentTypes;

    // The maximum allowed size for an attachment.
    @Getter private long maxAllowedAttachmentSizeInBytes;

    // The maximum allowed number of attachments.
    // If set to -1 the number isn't limited.
    @Getter private int maxAllowedNumberOfAttachments;

    @Getter @NonNull private FileTypeBucket fileTypeBucket;

    // The rules to use to filter HTML in mails.
    // The plugin uses the owasp.html framework to filter html.
    // We store them as boolean flags to be compatible with xstream/bandana storage
    // and to set/get them easily from velocity templates.
    // Using an ArrayList as storage proofed to be too troublesome.
    // Use getHtmlFilter() to get a policyFactory.
    @Getter @NonNull private boolean htmlFilterFormatting;
    @Getter @NonNull private boolean htmlFilterBlocks;
    @Getter @NonNull private boolean htmlFilterImages;
    @Getter @NonNull private boolean htmlFilterLinks;
    @Getter @NonNull private boolean htmlFilterStyles;
    @Getter @NonNull private boolean htmlFilterTables;

    // Builder class with default values.
    public static class Mail2BlogBaseConfigurationBuilder
    {
        private String defaultSpace = "";
        private SpaceRule[] spaceRules = new SpaceRule[]{};
        private String defaultContentType = ContentTypes.BlogPost;
        private String[] preferredContentTypes = new String[]{"text/html", "application/xhtml+xml", "text/plain"};
        private long maxAllowedAttachmentSizeInBytes = 1024 * 1024 * 100; // 100mb
        private int maxAllowedNumberOfAttachments = -1;
        private FileTypeBucket fileTypeBucket = FileTypeBucket.defaultBucket();

        private boolean htmlFilterFormatting = true;
        private boolean htmlFilterBlocks = true;
        private boolean htmlFilterImages = true;
        private boolean htmlFilterLinks = true;
        private boolean htmlFilterStyles = true;
        private boolean htmlFilterTables = true;
    }
}
