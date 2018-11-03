package de.dm.mail2blog.base;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpaceInfo {
    private String spaceKey;
    private String contentType; // ContentTypes
}
