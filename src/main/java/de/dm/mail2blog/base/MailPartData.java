package de.dm.mail2blog.base;

import lombok.Data;
import lombok.ToString;

import java.io.InputStream;

/**
 * Bean to store information about a part of an email (multipart emails).
 */
@Data @ToString(includeFieldNames=true)
public class MailPartData {

    private AttachementData attachementData = null;
    private InputStream stream = null;
    private String html = null;
    private String contentID = null;
    private String contentType = null;
}
