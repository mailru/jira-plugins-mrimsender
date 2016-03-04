package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class CalendarSettingDto {
    @XmlElement
    private Integer id;
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
    private Map<String, String> displayedFields;
    @XmlElement
    private List<String> selectedDisplayedFields;

    @XmlElement
    private boolean canAdmin;
    @XmlElement
    private List<PermissionItemDto> permissions;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSelectedName() {
        return selectedName;
    }

    public void setSelectedName(String selectedName) {
        this.selectedName = selectedName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerAvatarUrl() {
        return ownerAvatarUrl;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
    }

    public String getSelectedSourceId() {
        return selectedSourceId;
    }

    public void setSelectedSourceId(String selectedSourceId) {
        this.selectedSourceId = selectedSourceId;
    }

    public String getSelectedSourceName() {
        return selectedSourceName;
    }

    public void setSelectedSourceName(String selectedSourceName) {
        this.selectedSourceName = selectedSourceName;
    }

    public Long getSelectedSourceAvatarId() {
        return selectedSourceAvatarId;
    }

    public void setSelectedSourceAvatarId(Long selectedSourceAvatarId) {
        this.selectedSourceAvatarId = selectedSourceAvatarId;
    }

    public boolean isSelectedSourceIsUnavailable() {
        return selectedSourceIsUnavailable;
    }

    public void setSelectedSourceIsUnavailable(boolean selectedSourceIsUnavailable) {
        this.selectedSourceIsUnavailable = selectedSourceIsUnavailable;
    }

    public String getSelectedEventStartId() {
        return selectedEventStartId;
    }

    public void setSelectedEventStartId(String selectedEventStartId) {
        this.selectedEventStartId = selectedEventStartId;
    }

    public String getSelectedEventStartName() {
        return selectedEventStartName;
    }

    public void setSelectedEventStartName(String selectedEventStartName) {
        this.selectedEventStartName = selectedEventStartName;
    }

    public String getSelectedEventEndId() {
        return selectedEventEndId;
    }

    public void setSelectedEventEndId(String selectedEventEndId) {
        this.selectedEventEndId = selectedEventEndId;
    }

    public String getSelectedEventEndName() {
        return selectedEventEndName;
    }

    public void setSelectedEventEndName(String selectedEventEndName) {
        this.selectedEventEndName = selectedEventEndName;
    }

    public Map<String, String> getDisplayedFields() {
        return displayedFields;
    }

    public void setDisplayedFields(Map<String, String> displayedFields) {
        this.displayedFields = displayedFields;
    }

    public List<String> getSelectedDisplayedFields() {
        return selectedDisplayedFields;
    }

    public void setSelectedDisplayedFields(List<String> selectedDisplayedFields) {
        this.selectedDisplayedFields = selectedDisplayedFields;
    }

    public List<PermissionItemDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionItemDto> permissions) {
        this.permissions = permissions;
    }

    public void setCanAdmin(boolean canAdmin) {
        this.canAdmin = canAdmin;
    }
}
