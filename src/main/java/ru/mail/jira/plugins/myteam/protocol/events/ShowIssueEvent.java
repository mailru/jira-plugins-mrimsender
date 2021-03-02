/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.net.URL;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

@Getter
public class ShowIssueEvent {
  private final String chatId;
  private final String issueKey;
  private final String userId;
  private final boolean isGroupChat;

  public ShowIssueEvent(ChatMessageEvent chatMessageEvent, String jiraBaseUrl) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    this.isGroupChat = chatMessageEvent.getChatType().equals(ChatType.GROUP);

    String baseIssueLinkUrlPrefix = String.format("%s/browse/", jiraBaseUrl);
    URL url = Utils.tryFindUrlByPrefixInStr(chatMessageEvent.getMessage(), baseIssueLinkUrlPrefix);
    if (url == null) {
      this.issueKey =
          StringUtils.substringAfter(chatMessageEvent.getMessage().trim().toLowerCase(), "/issue")
              .trim();
    } else this.issueKey = StringUtils.substringAfterLast(url.getPath(), "/");
  }
}
