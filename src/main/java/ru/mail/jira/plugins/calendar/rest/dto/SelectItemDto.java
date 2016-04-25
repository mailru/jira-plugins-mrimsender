package ru.mail.jira.plugins.calendar.rest.dto;

import com.atlassian.jira.issue.search.SearchRequest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class SelectItemDto {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement
    private String type;
    @XmlElement
    private long avatarId;

    public SelectItemDto(String id, String text) {
        this(id, text, 0);
    }

    public SelectItemDto(String id, String text, long avatarId) {
        this.id = id;
        this.text = text;
        this.avatarId = avatarId;
    }

    public SelectItemDto(String id, String text, String type) {
        this.id = id;
        this.text = text;
        this.type = type;
    }

    public SelectItemDto(String id, String text, String type, long avatarId) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.avatarId = avatarId;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public long getAvatarId() {
        return avatarId;
    }

    public static  List<SelectItemDto> buildSelectSearchRequestItemDtos(Collection<SearchRequest> searchRequests) {
        List<SelectItemDto> result = new ArrayList<SelectItemDto>(searchRequests.size());
        for (SearchRequest search : searchRequests)
            result.add(new SelectItemDto(String.valueOf(search.getId()), search.getName(), 0));
        return result;
    }
}
