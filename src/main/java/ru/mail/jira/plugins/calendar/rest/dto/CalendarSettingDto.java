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
    private String selectedColor;
    @XmlElement
    private String selectedSourceType;
    @XmlElement
    private String selectedSourceValue;
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

    public String getSelectedColor() {
        return selectedColor;
    }

    public void setSelectedColor(String selectedColor) {
        this.selectedColor = selectedColor;
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

    public String getSelectedSourceType() {
        return selectedSourceType;
    }

    public void setSelectedSourceType(String selectedSourceType) {
        this.selectedSourceType = selectedSourceType;
    }

    public String getSelectedSourceValue() {
        return selectedSourceValue;
    }

    public void setSelectedSourceValue(String selectedSourceValue) {
        this.selectedSourceValue = selectedSourceValue;
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
