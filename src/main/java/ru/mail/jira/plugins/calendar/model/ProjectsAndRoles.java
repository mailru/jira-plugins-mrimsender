package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class ProjectsAndRoles {
    @XmlElement
    private Map<Long,String> projects;
    @XmlElement
    private Map<Long,Map<Long,String>> roles;

    public ProjectsAndRoles() { }

    public ProjectsAndRoles(Map<Long, String> projects, Map<Long,Map<Long,String>> roles) {
        this.projects = projects;
        this.roles = roles;
    }
}
