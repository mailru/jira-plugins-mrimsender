package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class DateField {
    @XmlElement
    private String id;
    @XmlElement
    private String name;

    public DateField() { }

    private DateField(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static DateField of(String id, String name) {
        return new DateField(id, name);
    }
}
