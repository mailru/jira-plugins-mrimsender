package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class IssueSourceDto {
    @XmlElement
    private List<SelectItemDto> projects;
    @XmlElement
    private List<SelectItemDto> filters;

    public IssueSourceDto(List<SelectItemDto> projects, List<SelectItemDto> filters) {
        this.projects = projects;
        this.filters = filters;
    }
}
