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
    private int totalProjectsCount;
    @XmlElement
    private List<SelectItemDto> filters;
    @XmlElement
    private int totalFiltersCount;

    public List<SelectItemDto> getProjects() {
        return projects;
    }

    public void setProjects(List<SelectItemDto> projects) {
        this.projects = projects;
    }

    public int getTotalProjectsCount() {
        return totalProjectsCount;
    }

    public void setTotalProjectsCount(int totalProjectsCount) {
        this.totalProjectsCount = totalProjectsCount;
    }

    public List<SelectItemDto> getFilters() {
        return filters;
    }

    public void setFilters(List<SelectItemDto> filters) {
        this.filters = filters;
    }

    public int getTotalFiltersCount() {
        return totalFiltersCount;
    }

    public void setTotalFiltersCount(int totalFiltersCount) {
        this.totalFiltersCount = totalFiltersCount;
    }
}
