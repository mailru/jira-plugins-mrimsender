package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class AllSources {
    @XmlElement
    private List<SourceField> projects;
    @XmlElement
    private List<SourceField> filters;

    public AllSources(List<SourceField> projects, List<SourceField> filters) {
        this.projects = projects;
        this.filters = filters;
    }
}
