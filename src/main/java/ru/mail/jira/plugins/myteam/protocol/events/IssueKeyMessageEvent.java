/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.net.URL;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.Utils;

@Getter
public class IssueKeyMessageEvent {
  private final String chatId;
  private final String issueKey;
  private final String userId;

  public IssueKeyMessageEvent(ChatMessageEvent chatMessageEvent, String jiraBaseUrl) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();

    String baseIssueLinkUrlPrefix = String.format("%s/browse/", jiraBaseUrl);
    URL url = Utils.tryFindUrlByPrefixInStr(chatMessageEvent.getMessage(), baseIssueLinkUrlPrefix);
    if (url == null) {
      this.issueKey = chatMessageEvent.getMessage().trim().toUpperCase();
    } else this.issueKey = StringUtils.substringAfterLast(url.getPath(), "/");
  }
}
