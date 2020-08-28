package ru.mail.jira.plugins.myteam.protocol.events;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.Utils;

import java.net.URL;


@Getter
public class ShowIssueEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public ShowIssueEvent(ChatMessageEvent chatMessageEvent, ApplicationProperties applicationProperties) {
        String baseIssueLinkUrlPrefix = String.format("%s/browse/", applicationProperties.getString(APKeys.JIRA_BASEURL));
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
        URL url = Utils.tryFindUrlByPrefixInStr(chatMessageEvent.getMessage(), baseIssueLinkUrlPrefix);
        if (url == null) {
            this.issueKey = StringUtils.substringAfter(chatMessageEvent.getMessage().trim().toLowerCase(),"/issue")
                                       .trim()
                                       .toUpperCase();
        }
        else
            this.issueKey = StringUtils.substringAfterLast(url.getPath(), "/");
    }


}
