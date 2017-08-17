package ru.mail.jira.plugins.calendar.schedule.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class UserDto {
    @XmlElement
    private String key;
    @XmlElement
    private String name;
    @XmlElement
    private String displayName;

    public UserDto(String key, String name, String displayName) {
        this.key = key;
        this.name = name;
        this.displayName = displayName;
    }
}
