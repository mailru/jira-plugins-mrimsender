package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
@Getter
@Setter
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
    private boolean editableSetting;

    @XmlElement
    private Map<String, String> displayedFields;
    @XmlElement
    private List<String> selectedDisplayedFields;
    @XmlElement
    private boolean showIssueStatus;

    @XmlElement
    private boolean canAdmin;
    @XmlElement
    private List<PermissionItemDto> permissions;
}
