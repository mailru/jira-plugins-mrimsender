package ru.mail.jira.plugins.calendar.rest.dto;

import ru.mail.jira.plugins.calendar.model.Calendar;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class CalendarOutput {
    @XmlElement
    private int id;
    @XmlElement
    private String name;
    @XmlElement
    private String color;
    @XmlElement
    private String owner;
    @XmlElement
    private String ownerFullName;
    @XmlElement
    private String ownerAvatarUrl;
    @XmlElement
    private String source;
    @XmlElement
    private boolean changable;
    @XmlElement
    private boolean visible;
    @XmlElement
    private boolean isMy;
    @XmlElement
    private boolean fromOthers;
    @XmlElement
    private boolean favorite;
    @XmlElement
    private boolean hasError;
    @XmlElement
    private String error;
    @XmlElement
    private Integer usersCount;

    public CalendarOutput() { }

    public CalendarOutput(Calendar calendar) {
        this.id = calendar.getID();
        this.name = calendar.getName();
        this.color = calendar.getColor();
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setChangable(boolean changable) {
        this.changable = changable;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setIsMy(boolean isMy) {
        this.isMy = isMy;
    }

    public void setFromOthers(boolean fromOthers) {
        this.fromOthers = fromOthers;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setHasError(boolean withErrors) {
        this.hasError = withErrors;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setUsersCount(Integer usersCount) {
        this.usersCount = usersCount;
    }

    public Integer getUsersCount() {
        return usersCount;
    }
}
