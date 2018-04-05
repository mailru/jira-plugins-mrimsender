package ru.mail.jira.plugins.calendar.service.applications;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @Setter
@XmlRootElement
public class SprintDto {
    @XmlElement
    private Long id;
    @XmlElement
    private String boardName;
    @XmlElement
    private String name;
}
