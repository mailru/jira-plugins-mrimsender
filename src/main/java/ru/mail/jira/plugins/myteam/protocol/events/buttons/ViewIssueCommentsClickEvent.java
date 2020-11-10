/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class ViewIssueCommentsClickEvent {
  private final String userId;
  private final String chatId;
  private final String queryId;
  private final String issueKey;

  public ViewIssueCommentsClickEvent(ButtonClickEvent chatButtonClickEvent) {
    this.userId = chatButtonClickEvent.getUserId();
    this.chatId = chatButtonClickEvent.getChatId();
    this.queryId = chatButtonClickEvent.getQueryId();
    this.issueKey = StringUtils.substringAfter(chatButtonClickEvent.getCallbackData(), "-");
  }
}
