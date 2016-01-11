package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class DateField {
    @XmlElement
    private String id;
    @XmlElement
    private String text;

    public DateField() { }

    private DateField(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public static DateField of(String id, String text) {
        return new DateField(id, text);
    }
}
