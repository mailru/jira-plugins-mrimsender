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
    private List<SelectItemDto> myFilters;
    @XmlElement
    private List<SelectItemDto> favouriteFilters;
    @XmlElement
    private List<SelectItemDto> filters;

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

    public List<SelectItemDto> getMyFilters() {
        return myFilters;
    }

    public void setMyFilters(List<SelectItemDto> myFilters) {
        this.myFilters = myFilters;
    }

    public List<SelectItemDto> getFavouriteFilters() {
        return favouriteFilters;
    }

    public void setFavouriteFilters(List<SelectItemDto> favouriteFilters) {
        this.favouriteFilters = favouriteFilters;
    }

    public List<SelectItemDto> getFilters() {
        return filters;
    }

    public void setFilters(List<SelectItemDto> filters) {
        this.filters = filters;
    }
}
