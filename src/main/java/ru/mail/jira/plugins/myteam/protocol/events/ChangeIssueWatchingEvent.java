/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import com.google.common.base.Splitter;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class ChangeIssueWatchingEvent {
  private final String chatId;
  private final String userId;
  public String queryId;
  private String issueKey;

  private boolean isWatching;

  public ChangeIssueWatchingEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    List<String> splitedCmd =
        Splitter.on("watch").splitToList(chatMessageEvent.getMessage().toLowerCase());

    if (splitedCmd.size() > 1) {
      this.isWatching = !splitedCmd.get(0).equals("/un");
      this.issueKey = splitedCmd.get(1).trim();
    }
  }

  public ChangeIssueWatchingEvent(ButtonClickEvent buttonClickEvent) {
    this.chatId = buttonClickEvent.getChatId();
    this.userId = buttonClickEvent.getUserId();
    this.issueKey = StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
    this.isWatching =
        StringUtils.substringBefore(buttonClickEvent.getCallbackData(), "-").equals("watch");
    this.queryId = buttonClickEvent.getQueryId();
  }
}
