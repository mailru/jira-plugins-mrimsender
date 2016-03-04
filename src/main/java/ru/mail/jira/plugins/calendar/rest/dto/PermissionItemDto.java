package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class PermissionItemDto {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement
    private String type;
    @XmlElement
    private String accessType;
    @XmlElement
    private String avatarUrl;

    public PermissionItemDto() {

    }

    public PermissionItemDto(String id, String text, String type, String accessType, String avatarUrl) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.accessType = accessType;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public String getAccessType() {
        return accessType;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
