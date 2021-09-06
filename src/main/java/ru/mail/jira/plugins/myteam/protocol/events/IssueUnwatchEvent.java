/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import com.google.common.base.Splitter;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class IssueUnwatchEvent {
  private final String chatId;
  private final String userId;
  public String queryId;
  private String issueKey;

  public IssueUnwatchEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    List<String> splitedCmd =
        Splitter.on("watch").splitToList(chatMessageEvent.getMessage().toLowerCase());

    if (splitedCmd.size() > 1) {
      this.issueKey = splitedCmd.get(1).trim();
    }
  }

  public IssueUnwatchEvent(ButtonClickEvent buttonClickEvent) {
    this.chatId = buttonClickEvent.getChatId();
    this.userId = buttonClickEvent.getUserId();
    this.issueKey = StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
    this.queryId = buttonClickEvent.getQueryId();
  }
}
