package ru.mail.jira.plugins.calendar.rest.dto;

import ru.mail.jira.plugins.calendar.model.QuickFilter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class QuickFilterDto {
    @XmlElement
    private int id;
    @XmlElement
    private boolean favourite;
    @XmlElement
    private String name;
    @XmlElement
    private String jql;
    @XmlElement
    private String description;
    @XmlElement
    private Boolean share;
    @XmlElement
    private int calendarId;
    @XmlElement
    private boolean canEdit;
    @XmlElement
    private boolean selected;

    public QuickFilterDto() {
    }

    public QuickFilterDto(QuickFilter quickFilter) {
        this.id = quickFilter.getID();
        this.name = quickFilter.getName();
        this.jql = quickFilter.getJql();
        this.description = quickFilter.getDescription();
        this.share = quickFilter.isShare();
        this.calendarId = quickFilter.getCalendarId();
    }

    public QuickFilterDto(QuickFilter quickFilter, boolean canEdit, boolean favourite) {
        this(quickFilter);
        this.favourite = favourite;
        this.canEdit = canEdit;
    }

    public QuickFilterDto(int id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isFavourite() {
        return this.favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJql() {
        return this.jql;
    }

    public void setJql(String jql) {
        this.jql = jql;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean isShare() {
        return this.share;
    }

    public void setShare(Boolean share) {
        this.share = share;
    }

    public int getCalendarId() {
        return this.calendarId;
    }

    public void setCalendarId(int calendarId) {
        this.calendarId = calendarId;
    }

    public boolean isCanEdit() {
        return this.canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
