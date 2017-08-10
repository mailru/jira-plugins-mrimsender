package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@Getter @Setter
public class UserDto {
    @XmlElement
    private String key;
    @XmlElement
    private String name;
    @XmlElement
    private String displayName;
    @XmlElement
    private String avatarUrl;
}
