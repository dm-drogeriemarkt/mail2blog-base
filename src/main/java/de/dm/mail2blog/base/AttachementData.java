package de.dm.mail2blog.base;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
public class AttachementData {
    private String filename;
    private String mediaType;
    private long fileSize;
    private Date creationDate;
    private Date lastModificationDate;
}
