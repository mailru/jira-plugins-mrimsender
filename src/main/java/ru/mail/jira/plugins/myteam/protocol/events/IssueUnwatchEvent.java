/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class IssueUnwatchEvent {
  private final String chatId;
  private final String userId;
  private final String issueKey;
  public String queryId;

  public IssueUnwatchEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    this.issueKey = StringUtils.substringAfter(chatMessageEvent.getMessage(), "unwatch").trim();
  }

  public IssueUnwatchEvent(ButtonClickEvent buttonClickEvent) {
    this.chatId = buttonClickEvent.getChatId();
    this.userId = buttonClickEvent.getUserId();
    this.issueKey = StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
    this.queryId = buttonClickEvent.getQueryId();
  }
}
