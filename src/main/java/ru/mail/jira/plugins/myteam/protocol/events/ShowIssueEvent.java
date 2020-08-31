package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.Utils;

import java.net.URL;


@Getter
public class ShowIssueEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public ShowIssueEvent(ChatMessageEvent chatMessageEvent, String jiraBaseUrl) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();

        String baseIssueLinkUrlPrefix = String.format("%s/browse/", jiraBaseUrl);
        URL url = Utils.tryFindUrlByPrefixInStr(chatMessageEvent.getMessage(), baseIssueLinkUrlPrefix);
        if (url == null) {
            this.issueKey = StringUtils.substringAfter(chatMessageEvent.getMessage().trim().toLowerCase(), "/issue")
                                       .trim();
        } else
            this.issueKey = StringUtils.substringAfterLast(url.getPath(), "/");
    }
}
