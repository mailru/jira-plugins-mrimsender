package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class CalendarFieldsOutput {
    @XmlElement
    private String selectedName;

    @XmlElement
    private String owner;
    @XmlElement
    private String ownerFullName;
    @XmlElement
    private String ownerAvatarUrl;
    @XmlElement
    private String selectedColor;
    @XmlElement
    private String selectedSourceId;
    @XmlElement
    private String selectedSourceName;
    @XmlElement
    private Long selectedSourceAvatarId;
    @XmlElement
    private boolean selectedSourceIsUnavailable;
    @XmlElement
    private String selectedEventStartId;
    @XmlElement
    private String selectedEventStartName;
    @XmlElement
    private String selectedEventEndId;
    @XmlElement
    private String selectedEventEndName;

    @XmlElement
    private Map<String,String> displayedFields;
    @XmlElement
    private List<String> selectedDisplayedFields;

    @XmlElement
    private Map<Long,String> projectsForShare;
    @XmlElement
    private Map<Long,Map<Long,String>> projectRolesForShare;

    @XmlElement
    private List<String> groups;

    @XmlElement
    private List<SelectedShare> selectedShares;

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public void setSelectedName(String selectedName) {
        this.selectedName = selectedName;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }

    public void setSelectedSourceId(String selectedSourceId) {
        this.selectedSourceId = selectedSourceId;
    }

    public void setSelectedSourceName(String selectedSourceName) {
        this.selectedSourceName = selectedSourceName;
    }

    public void setSelectedSourceAvatarId(Long selectedSourceAvatarId) {
        this.selectedSourceAvatarId = selectedSourceAvatarId;
    }

    public void setSelectedSourceIsUnavailable(boolean selectedSourceIsUnavailable) {
        this.selectedSourceIsUnavailable = selectedSourceIsUnavailable;
    }

    public void setSelectedEventStartId(String selectedEventStartId) {
        this.selectedEventStartId = selectedEventStartId;
    }

    public void setSelectedEventStartName(String selectedEventStartName) {
        this.selectedEventStartName = selectedEventStartName;
    }

    public void setSelectedEventEndId(String selectedEventEndId) {
        this.selectedEventEndId = selectedEventEndId;
    }

    public void setSelectedEventEndName(String selectedEventEndName) {
        this.selectedEventEndName = selectedEventEndName;
    }

    public void setSelectedDisplayedFields(List<String> selectedDisplayedFields) {
        this.selectedDisplayedFields = selectedDisplayedFields;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public void setProjectsForShare(Map<Long, String> projectsForShare) {
        this.projectsForShare = projectsForShare;
    }

    public void setProjectRolesForShare(Map<Long,Map<Long,String>> projectRolesForShare) {
        this.projectRolesForShare = projectRolesForShare;
    }

    public void setSelectedShares(List<SelectedShare> selectedShares) {
        this.selectedShares = selectedShares;
    }
}
