package de.dm.mail2blog.base;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@Slf4j
public class MessageParser {
    // Config to use.
    private Mail2BlogBaseConfiguration mail2BlogBaseConfiguration;

    // Message to parse.
    Message message;

    // The number of attachments processed.
    private int attachmentCounter = 0;

    public MessageParser(@NonNull Message message, @NonNull Mail2BlogBaseConfiguration mail2BlogBaseConfiguration) {
        this.mail2BlogBaseConfiguration = mail2BlogBaseConfiguration;
        this.message = message;
    }

    /**
     * Get the sender email address
     *
     * @return the email address as string or null if sender could not be determined
     */
    public String getSenderEmail()
    {
        // Extract sender mail address.
        Address[] addresses;
        try {
            addresses = message.getFrom();
        } catch (MessagingException me) {
            return null;
        }

        if (addresses.length < 1) {
            return null;
        }

        String mail = "";
        if (addresses[0] instanceof InternetAddress) {
            mail = ((InternetAddress) addresses[0]).getAddress();
        } else {
            try {
                InternetAddress internet_addresses[] = InternetAddress.parse(addresses[0].toString());
                if (internet_addresses.length > 0) {
                    mail = internet_addresses[0].getAddress();
                }
            } catch (AddressException ae) {
                return null;
            }
        }

        return mail;
    }

    /**
     * Analyse content of mail.
     */
    public List<MailPartData> getContent()
    throws MessageParserException
    {
        return extract(message);
    }

    /**
     * Get the charset listed in a "Content-Type" header.
     *
     * @param contentType The "Content-Type" header.
     * @return Returns the used charset or the default charset, if no information is found.
     */
    public Charset getCharsetFromHeader(String contentType) {
        Charset charset = null;

        Pattern pattern = Pattern.compile("charset=\"?([0-9a-zA-Z\\-]+)");
        Matcher match = pattern.matcher(contentType);
        if (match.find()) {
            String charsetName = match.group(1);
            try {
                charset = Charset.forName(charsetName);
            } catch (UnsupportedCharsetException e) {
                charset = null;
            }
        }

        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        return charset;
    }

    /**
     * Extract text and attachments from a given E-Mail, or a part of multipart E-Mail.
     *
     * @param part The E-Mail or part of an E-Mail to handle.
     * @return the extracted data.
     */
    private List<MailPartData> extract(Part part)
    {
        ArrayList<MailPartData> result = new ArrayList<MailPartData>();

        try {
            if (part.getContent() instanceof Multipart) {
                result.addAll(extractMultiPart((Multipart)part.getContent()));
            } else {
                result.add(extractPart(part));
            }
        }  catch (Exception e) {
            log.debug("Mail2Blog: failed to process part of message", e);
        }

        return result;
    }

    /**
     * Handle a mime multi part of an E-Mail. Chooses part with preferred contenttype in multipart/alternative.
     */
    private List<MailPartData> extractMultiPart(Multipart part) throws Exception
    {
        // Get the content of all body parts.
        ArrayList<List<MailPartData>> bodyPartData = new ArrayList<List<MailPartData>>(part.getCount());
        for (int i = 0; i < part.getCount(); i++) {
            bodyPartData.add(i, extract(part.getBodyPart(i)));
        }

        // In multipart alternative,
        // use part with preferred content type (html, text).
        boolean foundPreferred = false;
        ArrayList<MailPartData> dataWithPreferred = new ArrayList<MailPartData>();
        if (part.getContentType().toLowerCase().startsWith("multipart/alternative")) {
            for (String contentType : mail2BlogBaseConfiguration.getPreferredContentTypes()) {
                if (!foundPreferred) {
                    // Walk through all body parts and use body parts that contain
                    // one ore more parts with the preferred content type.
                    for (int i = 0; i < part.getCount(); i++) {
                        boolean bodyPartContainsPreferred = false;

                        for (MailPartData data : bodyPartData.get(i)) {
                            if (data.getContentType().toLowerCase().startsWith(contentType)) {
                                foundPreferred = true;
                                bodyPartContainsPreferred = true;
                            }
                        }

                        if (bodyPartContainsPreferred) {
                            dataWithPreferred.addAll(bodyPartData.get(i));
                        }
                    }
                }
            }
        }

        // Return data with preferred content type if this is a multipart/alternative.
        // If we can't find a preferred content type or if this is not a multipart/alternative
        // return all data.
        if (foundPreferred) {
            return dataWithPreferred;
        } else {
            ArrayList<MailPartData> result = new ArrayList<MailPartData>();
            for (List<MailPartData> list: bodyPartData) {
                result.addAll(list);
            }

            return result;
        }
    }

    /**
     * Extract text or info about an attachment from a given part of an E-Mail.
     */
    private MailPartData extractPart(Part part) throws Exception
    {
        String mimeType = part.getContentType();

        if (mimeType == null) {
            throw new Exception("failed to get the contentType from the mail");
        }

        mimeType = mimeType.toLowerCase();
        int index = mimeType.indexOf(';');
        if (index >= 0) { mimeType = mimeType.substring(0, index); }


        String m = mimeType.toLowerCase();
        if (m.equals("application/xhtml+xml") || m.equals("text/html") || m.equals("text/plain")) {
            return extractContent(part, mimeType);
        } else {
            return extractAttachment(part, mimeType);
        }
    }

    /**
     * Extract info about a content part form a given part of an email.
     */
    private MailPartData extractContent(Part part, String mimeType) throws Exception {
        MailPartData result = new MailPartData();
        result.setContentType(mimeType);

        Charset charset = getCharsetFromHeader(part.getContentType());

        BufferedReader reader = new BufferedReader(new InputStreamReader(part.getInputStream(), charset));
        String line;
        String html = "";
        while ((line = reader.readLine()) != null) {
            if (mimeType.equals("text/plain")) {
                html += escapeHtml4(line) + "<br />";
            } else {
                html += line;
            }
        }

        result.setHtml(html);

        return result;
    }

    /**
     * Extract info about an attachment from a given part of an email.
     */
    private MailPartData extractAttachment(Part part, String mimeType) throws Exception {
        MailPartData result = new MailPartData();
        result.setContentType(mimeType);

        int maxattachments = mail2BlogBaseConfiguration.getMaxAllowedNumberOfAttachments();
        if (maxattachments >= 0 && attachmentCounter >= maxattachments) {
            throw new Exception("maximum number of attachments exceeded");
        }

        // Get the filename.
        String filename = part.getFileName();

        if (filename == null) {
            log.debug("Mail2Blog: attachment with no filename, generating one");
            filename = UUID.randomUUID().toString();
        }

        // Sanitize file name.
        filename = mail2BlogBaseConfiguration.getFileTypeBucket().saneFilename(filename, mimeType);

        // Check that the mime type of the extension is allowed.
        if (!mail2BlogBaseConfiguration.getFileTypeBucket().checkMimeType(mimeType)) {
            throw new Exception("contentType forbidden");
        }

        // Read input stream into a byte array.
        byte[] bytes;
        long filesize = 0;
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            InputStream input = part.getInputStream();
            int bytesRead = 0;

            do {
                if (filesize > mail2BlogBaseConfiguration.getMaxAllowedAttachmentSizeInBytes()) {
                    throw new Exception("attachment larger than allowed");
                }

                byte[] buffer = new byte[1024];
                bytesRead = input.read(buffer);

                if (bytesRead > 0) {
                    output.write(buffer, 0, bytesRead);
                    filesize += bytesRead;
                }
            } while (bytesRead > 0);

            bytes = output.toByteArray();


            Date d = new Date();

            // Create new attachment.
            result.setAttachementData(AttachementData.builder()
                    .filename(filename)
                    .mediaType(mimeType)
                    .fileSize(filesize)
                    .creationDate(d)
                    .lastModificationDate(d)
                    .build());

            if (part instanceof MimeBodyPart) {
                MimeBodyPart mime = (MimeBodyPart) part;
                result.setContentID(mime.getContentID());
            }

            result.setStream(new ByteArrayInputStream(bytes));

            attachmentCounter++;

            return result;
        }
    }
}
