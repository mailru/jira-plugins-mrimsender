package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class SourceField {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement
    private long avatarId;

    public SourceField(String id, String text) {
        this(id, text, 0);
    }

    public SourceField(String id, String text, long avatarId) {
        this.id = id;
        this.text = text;
        this.avatarId = avatarId;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public long getAvatarId() {
        return avatarId;
    }

    @Override
    public String toString() {
        return "SourceField{" +
                "id='" + id + '\'' +
                ", text='" + text + '\'' +
                ", avatarId=" + avatarId +
                '}';
    }
}
